
# ImageSaver-Swing


### Hierarchy
#### Folder
- src: folder contains source code
- uploaded_image: folder contains uploaded_image
- tmp_deleted_images: folder temp contains deleted image (for undo, will be remove when app exit)
- dist: contains built jar
#### Files
- database.txt: backup database
- secret.keys : backup secret

### How to run
- install: mysql (link: https://www.javatpoint.com/how-to-install-mysql, port: 3306, user: root, password: 12345678)
- load database structure: mysql -u root -p imageSaverSwing <path to database.txt>
- run: java -jar <path to dist>/ImageSaver_Swing.jar
