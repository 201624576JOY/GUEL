package application;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert.AlertType;


public class MainController implements Initializable {
	@FXML
	private TreeView<PathItem> treeV;
	@FXML
	private MenuBar menu;
	@FXML
	private MenuItem newFile;
	@FXML
	private MenuItem openFile;
	@FXML
	private MenuItem newWindow;
	@FXML
	private MenuItem save;
	@FXML
	private MenuItem print;
	@FXML
	private TabPane mainTab;
	@FXML
	private TextField txtMsg;
	@FXML
	private Button btnAlert;
	@FXML
	private Button loadbtn;
	
	private String LoadPath;
	
	private Path rootPath;
	
	private StringProperty messageProp= new SimpleStringProperty();
	private ExecutorService service;
	public MainController() {
        treeV = new TreeView<>();
        treeV.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        service = Executors.newFixedThreadPool(5);
    }
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		//createTree(); 
		
		
		
		//디렉토리 로드 버튼 액션
		loadbtn.setOnAction((event) -> {
			DirectoryChooser directorychooser = new DirectoryChooser();
			directorychooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File choice = directorychooser.showDialog(null);
            if(choice == null || ! choice.isDirectory()) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setHeaderText("Could not open directory");
                alert.setContentText("The file is invalid.");

                alert.showAndWait();
            } else {
            	LoadPath = choice.getPath();
            	rootPath = Paths.get(LoadPath);
            	PathItem pathItem = new PathItem(rootPath);
                treeV.setRoot(createNode(pathItem));
                treeV.setCellFactory((TreeView<PathItem> p) -> {
                	final PathTreeCell cell = new PathTreeCell(messageProp);
                    setDragDropEvent(cell);
                    //set
                    return cell;
                });
            }
        
	   });
		//파일 오픈
		openFile.setOnAction(new EventHandler<ActionEvent>() {
		    public void handle(ActionEvent event) {
		        FileChooser newFileChooser = new FileChooser();
		        File newFile = newFileChooser.showOpenDialog(null);
		        if (newFile != null) {
		            openNewTab(newFile.getPath());
		        }
		    }
		});
		//새파일 추가
		newFile.setOnAction((event) -> {
			Tab tab = new Tab();
		    tab.setText("untitled"); //*.txt
		    TextArea textArea = new TextArea();
		    textArea.appendText("");
		    tab.setContent(textArea);
		    mainTab.getTabs().add(tab);
		});
		//저장
		save.setOnAction((event) -> {
			if (isTabExist()) {
	            FileChooser saveFileChooser = new FileChooser();
	            File saveFile = saveFileChooser.showSaveDialog(null);
	            if (saveFile != null) {
	              saveFile(saveFile);
	            }
	        }
		});
		//프린트
		print.setOnAction((event) -> {
			if (isTabExist()) {
		        PrinterJob printerJob = PrinterJob.createPrinterJob();
		        if (printerJob.showPrintDialog(null)){
		            boolean success = printerJob.printPage(mainTab.getSelectionModel().getSelectedItem().getContent());
		            if (success) {
		                printerJob.endJob();
		            }
		        }
		    }
        });
		//신경ㄴㄴ
		newWindow.setOnAction((event) -> {
	        System.out.println("newWindow clicked");
	        /*
			Stage stage = new Stage();
			stage.setTitle(mainTab.getSelectionModel().getSelectedItem().getText());
			TextArea textArea = (TextArea)mainTab.getSelectionModel().getSelectedItem().getContent();
			TextArea newArea = new TextArea(textArea.getText());
			newArea.setEditable(false);
			stage.setScene(new Scene(newArea, 1000, 800));
			stage.show();      
			
			*/
		});
		//트리 아이템 두번  클릭시
		treeV.setOnMouseClicked(new EventHandler<MouseEvent>()
		{
		    @Override
		    public void handle(MouseEvent mouseEvent)
		    {            
		        if(mouseEvent.getClickCount() == 2){
		        	TreeItem<PathItem> item = treeV.getSelectionModel().getSelectedItem();
		            String clickpath = getTreePath(item);
		            System.out.println("Selected Text : " + clickpath);
		            openNewTab(clickpath); 
		        }
		    }
		 });

 	}

	//파일 저장
		 
	private void saveFile(File saveFile) {
		
		TextArea textArea = (TextArea) mainTab.getSelectionModel().getSelectedItem().getContent();
	    try{
	      FileWriter writer = null;
	      writer = new FileWriter(saveFile);
	      writer.write(textArea.getText().replaceAll("\n", "\r\n"));
	      writer.close();
	      
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	
	}

	//탭 존제여부
	private boolean isTabExist() {
		return mainTab.getSelectionModel().getSelectedItem() != null;
	}


	//선택한 파일 탭에 추가
	public void openNewTab(String path){
		File txtFile = new File(path);
		
		Tab tab = new Tab();
	    tab.setText(txtFile.getName()); //*.txt
	    TextArea textArea = new TextArea();
    		 
	    BufferedReader br = null;
	    
	    try{
	      br = new BufferedReader(new InputStreamReader(new FileInputStream(txtFile)));
	      String line;
	      while((line = br.readLine()) != null){
	        textArea.appendText(line + "\n");
	      }
	      
	    } catch (FileNotFoundException e) {
	      e.printStackTrace();
	      
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	  
	    tab.setContent(textArea);
	    mainTab.getTabs().add(tab);
	 }

		
		
	//디렉토리로 트리 만들기	
	public TreeItem<String> getNodesForDirectory(File directory) {
		   //Returns a TreeItem representation of the specified directory
           TreeItem<String> root = new TreeItem<String>(directory.getName());
           for(File f : directory.listFiles()) {
               System.out.println("Loading " + f.getName());
               if(f.isDirectory()) { //Then we call the function recursively
                   root.getChildren().add(getNodesForDirectory(f));
               } else {
            	   if(FileExtension(f.getName())) {
            		   System.out.println(f.getName());
	                   root.getChildren().add(new TreeItem<String>(f.getName()));
            	   }
               }
           }
           return root;
	 }
	
	// 선택한 트리아이템 경로 구하기
	public String getTreePath(TreeItem<PathItem> item) {
		TreeItem<PathItem> cur_item = item;
		String path = "";
		
		while(true) {
			if(cur_item.getParent() == null)
				break;
			else {
				path = "\\"+cur_item.getValue()+ path;
				cur_item = cur_item.getParent();
			}
		}
		path = LoadPath + path ;
		
		return path;
	}

	
	
// 그냥 버튼연습  신경 ㄴㄴ
	public void clickHandler() {
		String msg = txtMsg.getText();
		
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Gondr 알림창");
		alert.setHeaderText(null);
		alert.setContentText(msg);
		
		alert.show();
	}
// 이것도 연습용 신경 ㄴㄴ
	/*public void createTree(String... rootItems) {
	    //create root
	    TreeItem<String> root1 = new TreeItem<>("Root1");
	    //root.setExpanded(true);
	    //create child
	    TreeItem<String> Child1 = new TreeItem<>("Child1");
	    TreeItem<String> Child2 = new TreeItem<>("Child2");
	    Child1.setExpanded(false);
	    Child2.setExpanded(false);
	    root1.getChildren().add(Child1);
	    root1.getChildren().add(Child2);
	    treeV.setRoot(root1);
	}*/
	
// 파일 확장자 구분 하는거 근데 filefilter에 이런 기능 있는듯
	public static boolean FileExtension(String name) {
        String fileName = name;
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1,
                fileName.length());
        final String[] extension = { "txt","jpg"};// 확장자 구별 
 
        int len = extension.length;
        for (int i = 0; i < len; i++) {
            if (ext.equalsIgnoreCase(extension[i])) {
                return true; 
            }
        }
        return false;
    }
	//drag and drop 구현
	private void setDragDropEvent(final PathTreeCell cell) {
        // The drag starts on a gesture source
        cell.setOnDragDetected(event -> {
            TreeItem<PathItem> item = cell.getTreeItem();
            if (item != null && item.isLeaf()) {
                Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                List<File> files = Arrays.asList(cell.getTreeItem().getValue().getPath().toFile());
                content.putFiles(files);
                db.setContent(content);
                event.consume();
            }
        });
        // on a Target
        cell.setOnDragOver(event -> {
            TreeItem<PathItem> item = cell.getTreeItem();
            if ((item != null && !item.isLeaf()) &&
                    event.getGestureSource() != cell &&
                    event.getDragboard().hasFiles()) {
                Path targetPath = cell.getTreeItem().getValue().getPath();
                PathTreeCell sourceCell = (PathTreeCell) event.getGestureSource();
                final Path sourceParentPath = sourceCell.getTreeItem().getValue().getPath().getParent();
                if (sourceParentPath.compareTo(targetPath) != 0) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
            }
            event.consume();
        });
        // on a Target
        cell.setOnDragEntered(event -> {
            TreeItem<PathItem> item = cell.getTreeItem();
            if ((item != null && !item.isLeaf()) &&
                    event.getGestureSource() != cell &&
                    event.getDragboard().hasFiles()) {
                Path targetPath = cell.getTreeItem().getValue().getPath();
                PathTreeCell sourceCell = (PathTreeCell) event.getGestureSource();
                final Path sourceParentPath = sourceCell.getTreeItem().getValue().getPath().getParent();
                if (sourceParentPath.compareTo(targetPath) != 0) {
                    cell.setStyle("-fx-background-color: powderblue;");
                }
            }
            event.consume();
        });
        // on a Target
        cell.setOnDragExited(event -> {
            //cell.setStyle("-fx-background-color: white");
            event.consume();
        });
        // on a Target
        cell.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                final Path source = db.getFiles().get(0).toPath();
                final Path target = Paths.get(
                        cell.getTreeItem().getValue().getPath().toAbsolutePath().toString(),
                        source.getFileName().toString());
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                    Platform.runLater(() -> {
                        BooleanProperty replaceProp = new SimpleBooleanProperty();
                        //CopyModalDialog dialog = new CopyModalDialog(stage, replaceProp);
                        replaceProp.addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                            if (newValue) {
                                FileCopyTask task = new FileCopyTask(source, target);
                                service.submit(task);
                            }
                        });
                    });
                } else {
                    FileCopyTask task = new FileCopyTask(source, target);
                    service.submit(task);
                    task.setOnSucceeded(value -> {
                        Platform.runLater(() -> {
                            TreeItem<PathItem> item = PathTreeItem.createNode(new PathItem(target));
                            cell.getTreeItem().getChildren().add(item);
                        });
                    });
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
        // on a Source
        cell.setOnDragDone(event -> {
            ;
        });
    }
	
    private TreeItem<PathItem> createNode(PathItem pathItem) {
        return PathTreeItem.createNode(pathItem);
    }

}
