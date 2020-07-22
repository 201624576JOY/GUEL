package application;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class PathTreeCell extends TreeCell<PathItem>{
    private TextField textField;
    private Path editingPath;
    private StringProperty messageProp;
    private ContextMenu dirMenu = new ContextMenu();
    private ContextMenu fileMenu = new ContextMenu();
    private static List<String> imageList;
    static {
        imageList = Arrays.asList("BMP", "DIB", "RLE", "JPG", "JPEG" , "JPE", "JFIF"
        , "GIF", "EMF", "WMF", "TIF", "TIFF", "PNG", "ICO");
    }

    public PathTreeCell(final StringProperty messageProp) {
        this.messageProp = messageProp;
        MenuItem expandMenu = new MenuItem("Expand");
        expandMenu.setOnAction((ActionEvent event) -> {
            getTreeItem().setExpanded(true);
        });
        
        MenuItem expandAllMenu = new MenuItem("Expand All");
        expandAllMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                expandTreeItem(getTreeItem());
            }
            private void expandTreeItem(TreeItem<PathItem> item) {
                if (item.isLeaf()){
                    return;
                }
                item.setExpanded(true);
                ObservableList<TreeItem<PathItem>> children = item.getChildren();
                children.stream().filter(child -> (!child.isLeaf()))
                    .forEach(child -> expandTreeItem(child));
            }
        });
        
        MenuItem addMenu = new MenuItem("Add Directory");
        addMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Path newDir = createNewDirectory();
                if (newDir != null) {
                    TreeItem<PathItem> addItem = PathTreeItem.createNode(new PathItem(newDir));
                    getTreeItem().getChildren().add(addItem);
                }
            }
            private Path createNewDirectory() {
                Path newDir = null;
                while (true) {
                    Path path = getTreeItem().getValue().getPath();
                    newDir = Paths.get(path.toAbsolutePath().toString(), "newDirectory" + String.valueOf(getItem().getCountNewDir()));
                    try {
                        Files.createDirectory(newDir);
                        break;
                    } catch (FileAlreadyExistsException ex) {
                        continue;
                    } catch (IOException ex) {
                        cancelEdit();
                        messageProp.setValue(String.format("Creating directory(%s) failed", newDir.getFileName()));
                        break;
                    }
                }
                    return newDir;
            }
        });
        //create new file(txt) 새로운 txt파일 생성
        MenuItem addtxtfile = new MenuItem("AddTXTFile");
        addtxtfile.setOnAction(new EventHandler<ActionEvent>() {
        	public void handle(ActionEvent t) {
        		Path newfile = createnewfile();
            	if(newfile != null) {
            		TreeItem<PathItem> addthetxtfile = PathTreeItem.createNode(new PathItem(newfile));
            		getTreeItem().getChildren().add(addthetxtfile);
            	}
        	}
        	private Path createnewfile() {
        		 Path newtxt = null;
                 while (true) {
                     Path path = getTreeItem().getValue().getPath();
                     newtxt = Paths.get(path.toAbsolutePath().toString(), "new.txt");
                     try {
                         Files.createFile(newtxt);
                         break;
                     } catch (FileAlreadyExistsException ex) {
                         continue;
                     } catch (IOException ex) {
                         cancelEdit();
                         messageProp.setValue(String.format("Creating file(%s) failed", newtxt.getFileName()));
                         break;
                     }
                 }
                     return newtxt;
        		
        	}
        });
        //파일 불러오기(하지만 지금은 막혀있음, 로컬 디렉토리에서 불러오는 것이기 때문에)
        MenuItem addfile = new MenuItem("AddFile");
        addfile.setOnAction(new EventHandler<ActionEvent>() {
        	public void handle(ActionEvent t) {
        		FileChooser filechooser = new FileChooser();
        		filechooser.setInitialDirectory(new File(System.getProperty("user.home")));
                File choice = filechooser.showSaveDialog(null);
                if(choice == null || ! choice.isDirectory()) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setHeaderText("Could not open directory");
                    alert.setContentText("The file is invalid.");

                    alert.showAndWait();
                } else {
                	String LoadPath = choice.getPath();
                	Path RootPath = Paths.get(LoadPath);
                	System.out.println(RootPath);
                	PathItem pathItem = new PathItem(RootPath);
                	TreeItem<PathItem> addthefile = PathTreeItem.createNode(pathItem);
            		getTreeItem().getChildren().add(addthefile);
                }
        	}
        });
        
        MenuItem deleteMenu =new MenuItem("Delete");
        deleteMenu.setOnAction((ActionEvent event) -> {
            ObjectProperty<TreeItem<PathItem>> prop = new SimpleObjectProperty<>();
            //new ModalDialog(owner, getTreeItem(), prop);
            prop.addListener((ObservableValue<? extends TreeItem<PathItem>> ov, TreeItem<PathItem> oldItem, TreeItem<PathItem> newItem) -> {
                try {
                    Files.walkFileTree(newItem.getValue().getPath(), new VisitorForDelete());
                    if (getTreeItem().getParent() == null){
                        // when the root is deleted how to clear the TreeView???
                    } else {
                        getTreeItem().getParent().getChildren().remove(newItem);
                    }
                } catch (IOException ex) {
                    messageProp.setValue(String.format("Deleting %s failed", newItem.getValue().getPath().getFileName()));
                }
            });
        });
        //현재 디렉토리에 있는 파일 이름 바꾸기(구현중)
        MenuItem rename = new MenuItem("rename");
        rename.setOnAction(new EventHandler<ActionEvent>() {
        	//rename the file
        	public void handle(ActionEvent t) {
        		/*Path renamefile = renamethefile();
        		if(renamefile != null) {
        			TreeItem<PathItem> renaming = PathTreeItem.r
        		}*/       		       		
        	}/*
        	private Path renamethefile() {
        		Path refile = null;
        		Path path = getTreeItem().getValue().getPath();
                refile = Paths.get(path.toAbsolutePath().toString(), "rename.txt");
                Files.
        	}*/
        });
        MenuItem copy = new MenuItem("copy");
        copy.setOnAction(new EventHandler<ActionEvent>() {
        	public void handle(ActionEvent t) {
        		Path copyfile  = getTreeItem().getValue().getPath();
            	if(copyfile != null) {
            		TreeItem<PathItem> copythefile = PathTreeItem.createNode(new PathItem(copyfile));
            		getTreeItem().getChildren().add(copythefile);
            	}
        	}        	
        });
        dirMenu.getItems().addAll(expandMenu, expandAllMenu, addMenu, addtxtfile, addfile);
        fileMenu.getItems().addAll(deleteMenu, rename, copy);
    }

    @Override
    protected void updateItem(PathItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            final String itemString = getString();
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(itemString);
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(itemString);
                final Node image = getImage(itemString);
                if (image != null) {
                    setGraphic(image); // <-- *** add image
                } else {
                    setGraphic(null);
                }
                if (!getTreeItem().isLeaf()) {
                    setContextMenu(dirMenu);
                } else {
                    setContextMenu(fileMenu);
                }
            }
        }
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (textField == null){
            createTextField();
        }
        setText(null);
        setGraphic(textField);
        textField.selectAll();
        if (getItem() == null) {
            editingPath = null;
        } else {
            editingPath =getItem().getPath();
        }
    }

    @Override
    public void commitEdit(PathItem pathItem) {
        // rename the file or directory
        if (editingPath != null) {
            try {
                Files.move(editingPath, pathItem.getPath());
            } catch (IOException ex) {
                cancelEdit();
                messageProp.setValue(String.format("Renaming %s filed", editingPath.getFileName()));
            }
        }
        super.commitEdit(pathItem);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getString());
        setGraphic(null);
    }

    private String getString() {
        return getItem().toString();
    }

    private void createTextField() {
        textField = new TextField(getString());
        textField.setOnKeyReleased((KeyEvent t) -> {
            if (t.getCode() == KeyCode.ENTER){
                Path path = Paths.get(getItem().getPath().getParent().toAbsolutePath().toString(), textField.getText());
                commitEdit(new PathItem(path));
            } else if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
    }

    private Node getImage(String itemPath) {
        String parentPath = getItem().getPath().getParent().toAbsolutePath().toString();
        Image image = new Image("file:" + parentPath + "/" + itemPath);
        if (image.isError()) {
            return null; // <-- if not image
        }
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setFitWidth(10); // <-- set size of image
        imageView.setPreserveRatio(true);
        return imageView;
    }
}
