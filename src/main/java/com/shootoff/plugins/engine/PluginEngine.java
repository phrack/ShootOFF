/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.plugins.engine;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootoff.plugins.BouncingTargets;
import com.shootoff.plugins.DuelingTree;
import com.shootoff.plugins.ISSFStandardPistol;
import com.shootoff.plugins.ParForScore;
import com.shootoff.plugins.ParRandomShot;
import com.shootoff.plugins.RandomShoot;
import com.shootoff.plugins.ShootDontShoot;
import com.shootoff.plugins.ShootForScore;
import com.shootoff.plugins.SpaceInvaders;
import com.shootoff.plugins.SteelChallenge;
import com.shootoff.plugins.TimedHolsterDrill;

/**
 * Watch for new plugin jars and manage plugin registration and deletion.
 * 
 * @author phrack
 */
public class PluginEngine implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(PluginEngine.class);
	private final Path pluginDir;
	private final PluginListener pluginListener;
	private final PathMatcher jarMatcher = FileSystems.getDefault().getPathMatcher("glob:*.jar");
	private final WatchService watcher = FileSystems.getDefault().newWatchService();
	private final Set<Plugin> plugins = new HashSet<Plugin>();

	private AtomicBoolean watching = new AtomicBoolean(false);

	public PluginEngine(final PluginListener pluginListener) throws IOException {
		if (pluginListener == null) {
			throw new IllegalArgumentException("pluginListener cannot be null");
		}

		pluginDir = Paths.get(System.getProperty("shootoff.plugins"));
		this.pluginListener = pluginListener;

		if (!Files.exists(pluginDir) && !pluginDir.toFile().mkdirs()) {
			logger.error("The path specified by shootoff.plugins doesn't exist and we couldn't create it.");
			return;
		}

		if (!Files.isDirectory(pluginDir)) {
			logger.error("Can't enumerate existing plugins or watch for new plugins because the "
					+ "shootoff.plugins property is not set to a directory");
		}

		registerDefaultStandardTrainingExercises();
		enumerateExistingPlugins();
		registerDefaultProjectorExercises();

		pluginDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
	}

	private void registerDefaultStandardTrainingExercises() {
		pluginListener.registerExercise(new ISSFStandardPistol());
		pluginListener.registerExercise(new RandomShoot());
		pluginListener.registerExercise(new ShootForScore());
		pluginListener.registerExercise(new TimedHolsterDrill());
		pluginListener.registerExercise(new ParForScore());
		pluginListener.registerExercise(new ParRandomShot());
	}

	private void registerDefaultProjectorExercises() {
		pluginListener.registerProjectorExercise(new SpaceInvaders());
		pluginListener.registerProjectorExercise(new BouncingTargets());
		pluginListener.registerProjectorExercise(new DuelingTree());
		pluginListener.registerProjectorExercise(new ShootDontShoot());
		pluginListener.registerProjectorExercise(new SteelChallenge());
	}

	private boolean registerPlugin(final Path jarPath) {
		final Plugin newPlugin;

		try {
			newPlugin = new Plugin(jarPath);
		} catch (Exception e) {
			logger.error("Error creating new plugin", e);
			return false;
		}

		if (plugins.add(newPlugin)) {
			if (PluginType.STANDARD.equals(newPlugin.getType())) {
				pluginListener.registerExercise(newPlugin.getExercise());
			} else if (PluginType.PROJECTOR_ONLY.equals(newPlugin.getType())) {
				pluginListener.registerProjectorExercise(newPlugin.getExercise());
			}
		}

		return true;
	}

	private void enumerateExistingPlugins() {
		try {
			Files.walk(pluginDir).forEach(filePath -> {
				if (Files.isRegularFile(filePath) && jarMatcher.matches(filePath.getFileName())) {
					registerPlugin(filePath);
				}
			});
		} catch (IOException e) {
			logger.error("Error enumerating existing external plugins", e);
		}
	}

	protected Set<Plugin> getPlugins() {
		return plugins;
	}

	/**
	 * Start watching for plugin creation and deletion in shootoff.plugins. Plugin
	 * jar creation or deletion leads to a plugin registration or unregistration.
	 */
	public void startWatching() {
		watching.set(true);
		new Thread(this, "Plugin Watcher").start();
	}

	/**
	 * Stop watching for plugin creation and deletion in shootoff.plugins.
	 */
	public void stopWatching() {
		watching.set(false);
	}

	@Override
	public void run() {
		logger.debug("Starting to watch plugins directory");

		while (watching.get()) {
			WatchKey key;
			try {
				key = watcher.poll(1, TimeUnit.SECONDS);

				if (key == null) continue;
			} catch (InterruptedException e) {
				logger.error("Plugin watcher service was interrupted", e);
				return;
			}

			for (final WatchEvent<?> event : key.pollEvents()) {
				if (StandardWatchEventKinds.OVERFLOW.equals(event.kind())) {
					continue;
				}

				@SuppressWarnings("unchecked")
				final WatchEvent<Path> ev = (WatchEvent<Path>) event;
				final Path updatedFile = ev.context();

				if (!jarMatcher.matches(updatedFile)) {
					continue;
				}

				final Path fqUpdatedFile = pluginDir.resolve(updatedFile);

				if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
					if (!registerPlugin(fqUpdatedFile)) continue;
				} else if (StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())) {
					Optional<Plugin> deletedPlugin = Optional.empty();

					for (final Plugin p : plugins) {
						if (p.getJarPath().equals(fqUpdatedFile)) {
							deletedPlugin = Optional.of(p);
							break;
						}
					}

					if (deletedPlugin.isPresent()) {
						pluginListener.unregisterExercise(deletedPlugin.get().getExercise());
						plugins.remove(deletedPlugin.get());
					}
				} else {
					logger.warn("Unexpected plugin watcher event {}", event.kind().toString());
				}
			}

			if (!key.reset()) {
				logger.error("Could not reset watch key, cannot receive further plugin watch events");
				watching.set(false);
			}
		}

		logger.debug("Stopped watching plugins directory");
	}
}
