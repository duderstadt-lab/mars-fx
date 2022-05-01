#@OUTPUT javafx.scene.Node node

import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.application.Platform

node = new BorderPane()

//Everything done on node needs to be done on the javafx thread
//Placing it inside the run method below accomplishes that
//Otherwise, it would run on the swing thread and might throw an error.
Platform.runLater(new Runnable() {
    @Override
    public void run() {
		def label = new Label("Hello Martians")
		node.setCenter(label)
    }
})