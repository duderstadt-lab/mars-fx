#@OUTPUT javafx.scene.Node node

from javafx.scene.control import Label
from javafx.scene.layout import BorderPane

label = Label("Hello Martians")
borderPane = BorderPane()
borderPane.setCenter(label)

node = borderPane