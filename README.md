# Gesture-Detector
Android library for detecting touch screen gestures.

Main feature include:

* Matrix transformation detector - calculates matrix transformaiton using Matrix.setPolyToPoly. Fast and efficient way of combining translation, scaling and rotation in one listener. 
* Other commonly used gestures: swipe, translace, scale, rotate
* Simple events: clicking, double clicking, long presses
* openGL compatibility - in case your vertical axis is fliped vertically (y2 = height - y)

How to use with gradle:

Add repository:

    maven {
        url "http://dl.bintray.com/st1hy/maven"
    }
    
Add dependecy to your module:

    compile 'org.st1hy.android:gesture-detector:1.0.3@aar'
    
I know it would be much better to publish it with source not as aar, however I haven't figgured it out yet (or I am just that lazy) so I am open to sugestions.

Its free to use, fork and whatnot, just don't blame my if it blows up or something.
