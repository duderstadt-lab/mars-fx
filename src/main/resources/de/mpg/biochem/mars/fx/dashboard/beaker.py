#@ MoleculeArchive archive
#@OUTPUT javafx.scene.Node node

import javafx.scene.control.Label
import javafx.scene.layout.BorderPane

label = Label("Hello Martians")
borderPane = BorderPane()
borderPane.setCenter(label)

node = borderPane