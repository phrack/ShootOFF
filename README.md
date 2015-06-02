ShootOFF
========

An open source, cross-platform framework to enhance laser dry fire training. The Shoot: Open Fire Framework (ShootOFF for short) runs on Linux, Mac, and Windows out of the box. See INSTALL for manual installation instructions for each platform.

You can find additional information about using ShootOFF on the wiki: https://github.com/phrack/ShootOFF/wiki

To work with the ShootOFF source code you will need, at a minimum:

* JDK 8 with JavaFX -- the easiest way to get this is to install the Oracle version of the JDK; OpenJDK does have the JavaFX source code, but the process to get a working JDK with it at the moment is time consuming. The minimum version for ShootOFF is 8u40. Any earlier version will not have the Dialog class (https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Dialog.html), which ShootOFF uses for alerts
* Gradle -- ensure the JDK you installed above is the default JDK, otherwise the build script will not be able to find the Ant tasks our build script uses for JavaFX deployment

To use Eclipse, we recommend installing E(fx)clipse and SceneBuilder as well. To generate an importable Eclipse project run: gradle eclipse

To create a runnable JAR file in build/dist (run with $ build/dist/java -jar ShootOFF.jar): gradle fxJar
