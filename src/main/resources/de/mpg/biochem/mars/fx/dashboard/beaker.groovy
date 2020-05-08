#@OUTPUT javafx.scene.Node node

import javafx.scene.control.Label
import javafx.scene.layout.BorderPane

def label = new Label("Hello Martians")
def borderPane = new BorderPane()
borderPane.setCenter(label)

node = borderPane