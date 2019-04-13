import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.Image;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileSystemView;
import javax.imageio.ImageIO;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.net.URL;
public class FileManager {
    public static final String APP_TITLE = "File Manager MDD Project ";
    private Desktop desktop;
    private FileSystemView fileSystemView;
    private File currentFile;
    private JPanel gui;
    private JTree tree;
    private DefaultTreeModel treeModel;
    private JTable table;
    private JProgressBar progressBar;
    private FileTableModel file_table_model;
    private ListSelectionListener listSelectionListener;
    private boolean cellSizesSet = false;
    private int rowIconPadding = 6;
    private JButton openFile;
    private JButton editFile;
    private JButton deleteFile;
    private JButton newFile;
 private JLabel fileName;
    private JTextField path;
    private JLabel date;
    private JLabel size;
    private JCheckBox readable;
    private JCheckBox writable;
    private JCheckBox executable;
    private JRadioButton isDirectory;
    private JRadioButton isFile;
    private JPanel newFilePanel;
    private JRadioButton newTypeFile;
    private JTextField name;
    public Container getGui() {
        if (gui==null) {
            gui = new JPanel(new BorderLayout(3,3));
            gui.setBorder(new EmptyBorder(5,5,5,5));
            fileSystemView = FileSystemView.getFileSystemView();
            desktop = Desktop.getDesktop();
            JPanel detailView = new JPanel(new BorderLayout(3,3));
            table = new JTable();
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.setShowVerticalLines(false);
            listSelectionListener = new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent lse) {
                    int row = table.getSelectionModel().getLeadSelectionIndex();
                    setFileDetails( ((FileTableModel)table.getModel()).getFile(row) );
                }
            };
            table.getSelectionModel().addListSelectionListener(listSelectionListener);
            JScrollPane tableScroll = new JScrollPane(table);
            Dimension d = tableScroll.getPreferredSize();
            tableScroll.setPreferredSize(new Dimension((int)d.getWidth(), (int)d.getHeight()/2));
            detailView.add(tableScroll, BorderLayout.CENTER);     
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            treeModel = new DefaultTreeModel(root);
            TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent tse){
                    DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode)tse.getPath().getLastPathComponent();
                    showChildren(node);
                    setFileDetails((File)node.getUserObject());
                }
            };         
            File[] roots = fileSystemView.getRoots();
            for (File fileSystemRoot : roots) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
                root.add( node );                
                File[] files = fileSystemView.getFiles(fileSystemRoot, true);
                for (File file : files) {
                    if (file.isDirectory()) {
                        node.add(new DefaultMutableTreeNode(file));                    }
                }              
            }
            tree = new JTree(treeModel);
            tree.setRootVisible(false);
            tree.addTreeSelectionListener(treeSelectionListener);
            tree.setCellRenderer(new FileTreeCellRenderer());
            tree.expandRow(0);
            JScrollPane treeScroll = new JScrollPane(tree);
            tree.setVisibleRowCount(15);
            Dimension preferredSize = treeScroll.getPreferredSize();
            Dimension widePreferred = new Dimension(
                200,
                (int)preferredSize.getHeight());
            treeScroll.setPreferredSize( widePreferred );
            JPanel fileMainDetails = new JPanel(new BorderLayout(4,2));
            fileMainDetails.setBorder(new EmptyBorder(0,6,0,6));
            JPanel fileDetailsLabels = new JPanel(new GridLayout(0,1,2,2));
            fileMainDetails.add(fileDetailsLabels, BorderLayout.WEST);
            JPanel fileDetailsValues = new JPanel(new GridLayout(0,1,2,2));
            fileMainDetails.add(fileDetailsValues, BorderLayout.CENTER);
            fileDetailsLabels.add(new JLabel("File", JLabel.TRAILING));
            fileName = new JLabel();
            fileDetailsValues.add(fileName);
            fileDetailsLabels.add(new JLabel("Path/name", JLabel.TRAILING));
            path = new JTextField(5);
            path.setEditable(false);
            fileDetailsValues.add(path);
            fileDetailsLabels.add(new JLabel("Last Modified", JLabel.TRAILING));
            date = new JLabel();
            fileDetailsValues.add(date);
            fileDetailsLabels.add(new JLabel("File size", JLabel.TRAILING));
            size = new JLabel();
            fileDetailsValues.add(size);
            fileDetailsLabels.add(new JLabel("Type", JLabel.TRAILING));
            JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEADING,4,0));
            isDirectory = new JRadioButton("Directory");
            isDirectory.setEnabled(false);
            flags.add(isDirectory);
            isFile = new JRadioButton("File");
            isFile.setEnabled(false);
            flags.add(isFile);
            fileDetailsValues.add(flags);
            int count = fileDetailsLabels.getComponentCount();
            for (int ii=0; ii<count; ii++) {
                fileDetailsLabels.getComponent(ii).setEnabled(false);
            }
            JToolBar toolBar = new JToolBar();           
            toolBar.setFloatable(false);
            openFile = new JButton("Open");
            openFile.setMnemonic('o');
            openFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    try {
                        desktop.open(currentFile);
                    } catch(Throwable t) {
                        showThrowable(t);
                    }
                    gui.repaint();
                }
            });
            toolBar.add(openFile);
            editFile = new JButton("Edit");
            editFile.setMnemonic('e');
            editFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    try {
                        desktop.edit(currentFile);
                    } catch(Throwable t) {
                        showThrowable(t);
                    }
                }
            });                    
            openFile.setEnabled(desktop.isSupported(Desktop.Action.OPEN));
            editFile.setEnabled(desktop.isSupported(Desktop.Action.EDIT));         
            toolBar.addSeparator();
            newFile = new JButton("New");
            newFile.setMnemonic('n');
            newFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    newFile();
                }
            });
            toolBar.add(newFile);       
            JButton renameFile = new JButton("Rename");
            renameFile.setMnemonic('r');
            renameFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    renameFile();
                }
            });
            toolBar.add(renameFile);
            deleteFile = new JButton("Delete");
            deleteFile.setMnemonic('d');
            deleteFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    deleteFile();
                }
            });
            toolBar.add(deleteFile);
            toolBar.addSeparator();
            readable = new JCheckBox("Read  ");
            readable.setMnemonic('a');            
            toolBar.add(readable);
            writable = new JCheckBox("Write  ");
            writable.setMnemonic('w');
            toolBar.add(writable);
            executable = new JCheckBox("Execute");
            executable.setMnemonic('x');           
            toolBar.add(executable);
            JPanel fileView = new JPanel(new BorderLayout(3,3));
            fileView.add(toolBar,BorderLayout.NORTH);
            fileView.add(fileMainDetails,BorderLayout.CENTER);
            detailView.add(fileView, BorderLayout.SOUTH);
            JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                treeScroll,
                detailView);
            gui.add(splitPane, BorderLayout.CENTER);
            JPanel simpleOutput = new JPanel(new BorderLayout(3,3));
            progressBar = new JProgressBar();
            simpleOutput.add(progressBar, BorderLayout.EAST);
            progressBar.setVisible(false);
            gui.add(simpleOutput, BorderLayout.SOUTH);
        }
        return gui;
    }
    public void showRootFile() {
        tree.setSelectionInterval(0,0);
    }
    private TreePath findTreePath(File find) {
        for (int ii=0; ii<tree.getRowCount(); ii++) {
            TreePath treePath = tree.getPathForRow(ii);
            Object object = treePath.getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)object;
            File nodeFile = (File)node.getUserObject();
            if (nodeFile==find) {
                return treePath;
            }
        }
        return null;
    }
    private void renameFile() {
        if (currentFile==null) {
            showErrorMessage("No file selected to rename.","Select File");
            return;
        }
        String renameTo = JOptionPane.showInputDialog(gui, "New Name");
        if (renameTo!=null) {
            try {
                boolean directory = currentFile.isDirectory();
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                DefaultMutableTreeNode parentNode =
                    (DefaultMutableTreeNode)parentPath.getLastPathComponent();

                boolean renamed = currentFile.renameTo(new File(
                    currentFile.getParentFile(), renameTo));
                if (renamed) {
                    if (directory) {
                       
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode =
                            (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                        treeModel.removeNodeFromParent(currentNode);                   
                    }
                    showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                        currentFile +
                        "' could not be renamed.";
                    showErrorMessage(msg,"Rename Failed");
                }
            } catch(Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }
    private void deleteFile() {
        if (currentFile==null) {
            showErrorMessage("No file selected for deletion.","Select File");
            return;
        }
        int result = JOptionPane.showConfirmDialog(
            gui,
            "Are you sure you want to delete this file?",
            "Delete File",
            JOptionPane.ERROR_MESSAGE
            );
        if (result==JOptionPane.OK_OPTION) {
            try {
                System.out.println("currentFile: " + currentFile);
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                System.out.println("parentPath: " + parentPath);
                DefaultMutableTreeNode parentNode =
                    (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                System.out.println("parentNode: " + parentNode);
                boolean directory = currentFile.isDirectory();
                boolean deleted = currentFile.delete();
                if (deleted) {
                    if (directory) {                      
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode =
                            (DefaultMutableTreeNode)currentPath.getLastPathComponent();
                        treeModel.removeNodeFromParent(currentNode);
                    }
                    showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                        currentFile +
                        "' could not be deleted.";
                    showErrorMessage(msg,"Delete Failed");
                }
            } catch(Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }
    private void newFile() {
        if (currentFile==null) {
            showErrorMessage("No location selected for new file.","Select Location");
            return;
        }
        if (newFilePanel==null) {
            newFilePanel = new JPanel(new BorderLayout(3,3));
            JPanel south = new JPanel(new GridLayout(1,0,2,2));
            newTypeFile = new JRadioButton("File", true);
            JRadioButton newTypeDirectory = new JRadioButton("Directory");
            ButtonGroup bg = new ButtonGroup();
            bg.add(newTypeFile);
            bg.add(newTypeDirectory);
            south.add( newTypeFile );
            south.add( newTypeDirectory );
            name = new JTextField(15);
            newFilePanel.add( new JLabel("Name"), BorderLayout.WEST );
            newFilePanel.add( name );
            newFilePanel.add( south, BorderLayout.SOUTH );
        }
        int result = JOptionPane.showConfirmDialog(
            gui,
            newFilePanel,
            "Create File",
            JOptionPane.OK_CANCEL_OPTION);
        if (result==JOptionPane.OK_OPTION) {
            try {
                boolean created;
                File parentFile = currentFile;
                if (!parentFile.isDirectory()) {
                    parentFile = parentFile.getParentFile();
                }
                File file = new File( parentFile, name.getText() );
                if (newTypeFile.isSelected()) {
                    created = file.createNewFile();
                } else {
                    created = file.mkdir();
                }
                if (created) {
                    TreePath parentPath = findTreePath(parentFile);
                    DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                    if (file.isDirectory()) {                        
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(file);
                        TreePath currentPath = findTreePath(currentFile);
                        DefaultMutableTreeNode currentNode =
                            (DefaultMutableTreeNode)currentPath.getLastPathComponent();
                        treeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
                    }
                    showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                        file +
                        "' could not be created.";
                    showErrorMessage(msg, "Create Failed");
                }
            } catch(Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }
    private void showErrorMessage(String errorMessage, String errorTitle) {
        JOptionPane.showMessageDialog(
            gui,
            errorMessage,
            errorTitle,
            JOptionPane.PLAIN_MESSAGE
            );
    }
    private void showThrowable(Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(
            gui,
            t.toString(),
            t.getMessage(),
            JOptionPane.ERROR_MESSAGE
            );
        gui.repaint();
    }
    private void setTableData(final File[] files) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (file_table_model==null) {
                    file_table_model = new FileTableModel();
                    table.setModel(file_table_model);
                }
                table.getSelectionModel().removeListSelectionListener(listSelectionListener);
                file_table_model.setFiles(files);
                table.getSelectionModel().addListSelectionListener(listSelectionListener);
                if (!cellSizesSet) {
                    Icon icon = fileSystemView.getSystemIcon(files[0]);                 
                    table.setRowHeight( icon.getIconHeight()+rowIconPadding );
                    setColumnWidth(0,-1);
                    setColumnWidth(3,60);
                    table.getColumnModel().getColumn(3).setMaxWidth(120);
                    setColumnWidth(4,-1);
                    setColumnWidth(5,-1);
                    setColumnWidth(6,-1);
                    setColumnWidth(7,-1);
                    setColumnWidth(8,-1);
                    setColumnWidth(9,-1);
                    cellSizesSet = true;
                }
            }
        });
    }
    private void setColumnWidth(int column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        if (width<0) {          
            JLabel label = new JLabel( (String)tableColumn.getHeaderValue() );
            Dimension preferred = label.getPreferredSize();           
            width = (int)preferred.getWidth()+14;
        }
        tableColumn.setPreferredWidth(width);
        tableColumn.setMaxWidth(width);
        tableColumn.setMinWidth(width);
    }
    private void showChildren(final DefaultMutableTreeNode node) {
        tree.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
            @Override
            public Void doInBackground() {
                File file = (File) node.getUserObject();
                if (file.isDirectory()) {
                    File[] files = fileSystemView.getFiles(file, true); 
                    if (node.isLeaf()) {
                        for (File child : files) {
                            if (child.isDirectory()) {
                                publish(child);
                            }
                        }
                    }
                    setTableData(files);
                }
                return null;
            }
            @Override
            protected void process(List<File> chunks) {
                chunks.forEach((child) -> {
                    node.add(new DefaultMutableTreeNode(child));
                });
            }
            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                tree.setEnabled(true);
            }
        };
        worker.execute();
    }
    private void setFileDetails(File file) {
        currentFile = file;
        Icon icon = fileSystemView.getSystemIcon(file);
        fileName.setIcon(icon);
        fileName.setText(fileSystemView.getSystemDisplayName(file));
        path.setText(file.getPath());
        date.setText(new Date(file.lastModified()).toString());
        size.setText(file.length() + " bytes");
        readable.setSelected(file.canRead());
        writable.setSelected(file.canWrite());
        executable.setSelected(file.canExecute());
        isDirectory.setSelected(file.isDirectory());
        isFile.setSelected(file.isFile());
        JFrame f = (JFrame)gui.getTopLevelAncestor();
        if (f!=null) {
            f.setTitle(
                APP_TITLE +
                " :: " +
                fileSystemView.getSystemDisplayName(file) );
        }
        gui.repaint();
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch(Exception weTried) {
                }
                JFrame f = new JFrame(APP_TITLE);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                FileManager fileManager = new FileManager();
                f.setContentPane(fileManager.getGui());
                try {
                    URL urlBig = fileManager.getClass().getResource("fm-icon-32x32.png");
                    URL urlSmall = fileManager.getClass().getResource("fm-icon-16x16.png");
                    ArrayList<Image> images = new ArrayList<Image>();
                    images.add( ImageIO.read(urlBig) );
                    images.add( ImageIO.read(urlSmall) );
                    f.setIconImages(images);
                } catch(Exception weTried) {}

                f.pack();
                f.setLocationByPlatform(true);
                f.setMinimumSize(f.getSize());
                f.setVisible(true);
                fileManager.showRootFile();
            }
        });
    }
}
class FileTableModel extends AbstractTableModel {
    private File[] files;
    private FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private String[] columns = {
        "Icon",
        "File",    
    };
    FileTableModel() {
        this(new File[0]);
    }
    FileTableModel(File[] files) {
        this.files = files;
    }
    public Object getValueAt(int row, int column) {
        File file = files[row];
        switch (column) {
            case 0:
                return fileSystemView.getSystemIcon(file);
            case 1:
                return fileSystemView.getSystemDisplayName(file);
            case 2:
                return file.getPath();
            case 3:
                return file.length();
            case 4:
                return file.lastModified();
            case 5:
                return file.canRead();
            case 6:
                return file.canWrite();
            case 7:
                return file.canExecute();
            case 8:
                return file.isDirectory();
            case 9:
                return file.isFile();
            default:
                System.err.println("Logic Error");
        }
        return "";
    }
    @Override
    public int getColumnCount() {
        return columns.length;
    }
    @Override
  public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
                return ImageIcon.class;
            case 3:
                return Long.class;
            case 4:
                return Date.class;
            case 9:
                return Boolean.class;
        }
        return String.class;
    }
    @Override
    public String getColumnName(int column) {
        return columns[column];
    }
    @Override
    public int getRowCount() {
        return files.length;
    }
    public File getFile(int row) {
        return files[row];
    }
    public void setFiles(File[] files) {
        this.files = files;
        fireTableDataChanged();
    }
}
class FileTreeCellRenderer extends DefaultTreeCellRenderer {
    private final FileSystemView fileSystemView;
  FileTreeCellRenderer() { 
        fileSystemView = FileSystemView.getFileSystemView();
    }   
}
