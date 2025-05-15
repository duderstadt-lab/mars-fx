#@ Context scijavaContext
#@ MoleculeArchive archive
#@OUTPUT String[] imgsrcs

//Insert your code here for extraction of archive properties and image creation.

String dotImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg=="

images = []
images.add(dotImage)
images.add(dotImage)

imgsrcs = images as String[]