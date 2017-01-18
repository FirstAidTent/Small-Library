/*
 * SmallLibrary
 * 
 * Version 1.0
 * 
 */

import javax.swing.*;
import javax.swing.event.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SmallLibrary {
	static Connection DBConnect;
	static String DBName;
	static String BName = "";
	
	static int Number = 0;
	static String Selection = "";
	static String Deselection = "";
	static JFrame LibraryFrame = new JFrame("Library");
	static JPanel PanelN = new JPanel();
	static JList<String> BookList;
	static JList<String> BorrowList;
	static DefaultListModel<String> BookListM = new DefaultListModel<String>();
	static DefaultListModel<String> BorrowListM = new DefaultListModel<String>();
	static JScrollPane pane;
	static JScrollPane pane2;
	static SimpleDateFormat	df = new SimpleDateFormat("yyMMdd");
	
	public static void main(String[] args) {
		String				path				= "";
		String				CurrentLine			= "";
		String[]			SplitLine			= new String[10];
		BufferedReader		in					= null;
		BufferedWriter 		ErrorOut			= null;
		int					LineCount			= 0;
		int					d					= 0;
		Boolean				Default				= false;
		String				Error				= "";
		Boolean				IsbnValid			= false;
		Boolean				login				= false;
		Boolean[]			ErrorLine			= null;
		Boolean[]			BorrowedBook		= null;
		Statement			statement			= null;
		
		// Declaration of the attributes of the books
		// Variables with "S" in the end is used in case the value is not an integer/date
		String[]			Isbn				= null;
		int[]				CopyNumber			= null;
		String[]			CopyNumberS			= null;
		String[]			Title				= null;
		String[]			Author				= null;
		String[]			Publisher			= null;
		int[]				Year				= null;
		String[]			YearS				= null;
		int[]				Statistics			= null;
		String[]			StatisticsS			= null;
		Date[]				BorrowDate			= null;
		String[]			BorrowDateS			= null;
		Date[]				ReturnDate			= null;
		String[]			ReturnDateS			= null;
		int[]				LibraryCardNumber	= null;
		String[]			LibraryCardNumberS	= null;
		
		try {
			
			
			
		    //_________________________________________________________________________________________________________//
		    //																										   //
		    // 									Searching, checking and storing of Books							   //
		    //_________________________________________________________________________________________________________//
			
			
			// This is for if you want to skip over all initializations 
			d = JOptionPane.showConfirmDialog(null, "Do you want to use the default values?\n", "Default", JOptionPane.YES_NO_CANCEL_OPTION);
			
			if (d == 0) {
				Default = true;
			}
			
			if (d == 2) {
				return;
			}
			
			
			// Asking for path where you can find 'Books.txt'
			do {
				try {
					path = "";
					if (Default) {
						path = "src/";
					} else {
						path = JOptionPane.showInputDialog(null, "Please enter the path where 'Books.txt' can be found.\n" +
																"Example: 'C:/Users/Peter/Desktop/'", "Filepath for Books", 
																JOptionPane.PLAIN_MESSAGE);
					}
					if (path == null) {
						// If the user clicks cancel in the InputDialog or closes the window, the program ends
						return;
					}
					in = new BufferedReader(new FileReader(path + "Books.txt"));
					// If 'Books.txt' isn't found, the line below will be skipped over
					// by the catch statement, making the loop continue
					// If 'Books.txt' is found, the loop will be broken by the break statement
					break;
				} catch (FileNotFoundException e) {
					JOptionPane.showMessageDialog(null, "Books.txt not found. Try again.");
				}
			} while (true);
			
			// Asking user for database name to use
			if (Default) {
				DBName = "library";
			} else {
				DBName = JOptionPane.showInputDialog(null, "Please enter name of database to use.",
													 	"Database name", JOptionPane.PLAIN_MESSAGE);
			}
			if (DBName == null) {
				// If the user clicks cancel in the InputDialog or closes the window, the program ends
				in.close();
				return;
			}
		    
			// This part is to check how many books (lines) there are in books.txt
		    while ((CurrentLine = in.readLine()) != null) {
		        LineCount++;
		    }
		    in.close(); // Preventing resource leaks
		    
		    // Defining the array size of all the attributes of
		    // the books depending on number of books (lines)
		    Isbn				= new String[LineCount];
		    CopyNumber			= new int[LineCount];
		    CopyNumberS			= new String[LineCount];
			Title				= new String[LineCount];
			Author				= new String[LineCount];
			Publisher			= new String[LineCount];
			Year				= new int[LineCount];
			YearS				= new String[LineCount];
			Statistics			= new int[LineCount];
			StatisticsS			= new String[LineCount];
			BorrowDate			= new Date[LineCount];
			BorrowDateS			= new String[LineCount];
			ReturnDate			= new Date[LineCount];
			ReturnDateS			= new String[LineCount];
			LibraryCardNumber	= new int[LineCount];
			LibraryCardNumberS	= new String[LineCount];
			
			ErrorLine			= new Boolean[LineCount];
			BorrowedBook		= new Boolean[LineCount];
			for (int i = 0; i < LineCount; i++) {
				ErrorLine[i] = false;
			}
			for (int i = 0; i < LineCount; i++) {
				BorrowedBook[i] = true;
			}
			
			CurrentLine = "";
			LineCount = 0;
			df.setLenient(false);
			
			in = new BufferedReader(new FileReader(path + "Books.txt")); // Reading Books.txt from path specified
			ErrorOut = new BufferedWriter(new FileWriter(path + "ErrorLines.txt")); // Creating file ErrorLines.txt in path specified
			
			// Connecting to database
    		try {
    			Class.forName("com.mysql.jdbc.Driver");
    		} catch (ClassNotFoundException cnfe) {
    			System.err.println(cnfe);
    			ErrorOut.close();
				return;
    		}
    		
    		try{
    			DBConnect = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "");
    			statement = DBConnect.createStatement();
    		} catch (SQLException sqle) {
    			System.err.println("Error connecting to db: " + sqle.getMessage());
    			ErrorOut.close();
				return;
    		}
    		
    		// Checking if Database exists
    		try {
    			statement.executeUpdate("USE "+ DBName + ";");
    			JOptionPane.showMessageDialog(null, "Using " + DBName.toLowerCase() + " as database");
    			
    		} catch (SQLException sqle) {
    			JOptionPane.showMessageDialog(null, "Database " + DBName.toLowerCase() + " not found. The database will be created.");
    			try {
    				// Creating Database and Tables if Database does not already exist
					statement.executeUpdate("CREATE DATABASE " + DBName + ";");
					statement.executeUpdate("USE "+ DBName + ";");
		    		try {
						statement.executeUpdate("CREATE TABLE Books(" +
													"Isbn varchar(15) NOT NULL, " +
													"Copy_Number Integer(5), " +
													"Title varchar(100), " +
													"Author varchar(30), " +
													"Publisher varchar(30), " +
													"Year Integer(4), " +
													"Statistics Integer(2), " +
													"Borrow_Date Date, " +
													"Return_Date Date, " +
													"Library_Card_Num Integer(5), " +
													"PRIMARY KEY(Isbn)" +
												");");
						statement.executeUpdate("CREATE TABLE Borrowers(" +
													"Library_Card_Num Integer(10) NOT NULL, " +
													"Name varchar(30), " +
													"Street varchar(30), " +
													"Zip_code Integer(6), " +
													"Town varchar(30), " +
													"PRIMARY KEY(Library_Card_Num)" +
												");");
					} catch (SQLException e) {
						JOptionPane.showMessageDialog(null, "Could not create table. Quitting...");
						ErrorOut.close();
						return;
					}
				} catch (SQLException sqlee) {
					JOptionPane.showMessageDialog(null, "Could not create database. Quitting...");
					ErrorOut.close();
					return;
				}
    		}
    		
			// This while-statement below checks if a line in the text file contains a string and
			// ends when it fails to find a string
		    while ((CurrentLine = in.readLine()) != null) {
		        try {
		        	// The line bellow splits a string with the argument as the divider
		        	// and assign the parts of the string to a string-array
		        	SplitLine = CurrentLine.split("#");
		        	Error = "";
		        	
		        	// Isbn-number checking
		        	IsbnValid = true;
		        	Isbn[LineCount]	= SplitLine[0];
		        	if ((SplitLine[0].startsWith("978") || SplitLine[0].startsWith("979"))
		        			&& (Isbn[LineCount].length() == 13)) {
			        	try {
				        	Long.parseLong(SplitLine[0]);
				        	IsbnValid = isISBN13Valid(SplitLine[0]); // Method is declared at the end of this file
				        } catch (NumberFormatException nfe) {
				        	IsbnValid = false;
				        }
		        	} else {
		        		IsbnValid = false;
		        	}
		        	if (IsbnValid == false) {
		        		Error = " Isbn-Number";
		        		ErrorLine[LineCount] = true;
		        	}
		        	
		        	// Copy number checking
		        	try {
		        		CopyNumber[LineCount] = Integer.parseInt(SplitLine[1]);
		        	} catch (NumberFormatException nfe) {
		        		Error = Error + " Copy Number";
		        		ErrorLine[LineCount] = true;
		        		CopyNumberS[LineCount] = SplitLine[1];
			        }
		        	
		        	// Title checking
		        	Title[LineCount] = SplitLine[2];
		        	if (Title[LineCount].isEmpty()) {
		        		Error = Error + " Title";
		        		ErrorLine[LineCount] = true;
		        	}
		        	
		        	// Author checking
		        	Author[LineCount] = SplitLine[3];
		        	if (Author[LineCount].isEmpty()) {
		        		Error = Error + " Author";
		        		ErrorLine[LineCount] = true;
		        	}
		        	
		        	// Publisher checking
		        	Publisher[LineCount] = SplitLine[4];
		        	if (Publisher[LineCount].isEmpty()) {
		        		Error = Error + " Publisher";
		        		ErrorLine[LineCount] = true;
		        	}
		        	
		        	// Year checking
		        	try {
		        		Year[LineCount]	= Integer.parseInt(SplitLine[5]);
		        	} catch (NumberFormatException nfe) {
		        		Error = Error + " Year";
		        		ErrorLine[LineCount] = true;
		        		YearS[LineCount] = SplitLine[5];
			        }
		        	
		        	// Statistics checking
		        	try {
		        		Statistics[LineCount] = Integer.parseInt(SplitLine[6]);
		        	} catch (NumberFormatException nfe) {
		        		Error = Error + " Statistics";
		        		ErrorLine[LineCount] = true;
		        		StatisticsS[LineCount] = SplitLine[6];
			        }
		        	
		        	// Borrow Date checking
		        	try {
		    			BorrowDate[LineCount] = df.parse(SplitLine[7]);
		    		} catch (ParseException e1) {
		    			Error = Error + " BorrowDate";
		    			ErrorLine[LineCount] = true;
		    			BorrowDateS[LineCount] = SplitLine[7];
		    		}
		        	
		        	// Return Date checking, if available
		        	try {
		    			ReturnDate[LineCount] = df.parse(SplitLine[8]);
		    		} catch (ParseException e1) {
		    			Error = Error + " ReturnDate";
		    			ErrorLine[LineCount] = true;
		    			ReturnDateS[LineCount] = SplitLine[8];
		    		}
		        	
		        	// Library Card Number checking if available
		        	try {
		        		LibraryCardNumber[LineCount] = Integer.parseInt(SplitLine[9]);
		        	} catch (NumberFormatException nfe) {
		        		Error = Error + " Library Card Number";
		        		ErrorLine[LineCount] = true;
		        		LibraryCardNumberS[LineCount] = SplitLine[9];
			        }
		        	
		        } catch (ArrayIndexOutOfBoundsException e) {
		        	BorrowedBook[LineCount] = false;
		    	}
		        
	        	// Printing out a text-file with error-lines
		        if (ErrorLine[LineCount]) {
		        	ErrorOut.write("Wrong" + Error);
					ErrorOut.newLine();
					ErrorOut.write(Isbn[LineCount] + "#");
					if (CopyNumber[LineCount] != 0) {
						ErrorOut.write(CopyNumber[LineCount] + "#");
					} else {
						ErrorOut.write(CopyNumberS[LineCount] + "#");
					}
					ErrorOut.write(Title[LineCount] + "#");
					ErrorOut.write(Author[LineCount] + "#");
					ErrorOut.write(Publisher[LineCount] + "#");
					if (Year[LineCount] != 0) {
						ErrorOut.write(Year[LineCount] + "#");
					} else {
						ErrorOut.write(YearS[LineCount] + "#");
					}
					if (Statistics[LineCount] != 0) {
						ErrorOut.write(Statistics[LineCount] + "#");
					} else {
						ErrorOut.write(StatisticsS[LineCount] + "#");
					}
					if (BorrowedBook[LineCount]) {
						try {
							if (BorrowDate[LineCount] == null) {
								ErrorOut.write(BorrowDateS[LineCount] + "#");
							} else {
								ErrorOut.write(df.format(BorrowDate[LineCount]) + "#");
							}
						} catch (NullPointerException e) {
							ErrorOut.write("#");
						}
						try {
							if (ReturnDate[LineCount] == null) {
								ErrorOut.write(ReturnDateS[LineCount] + "#");
							} else {
								ErrorOut.write(df.format(ReturnDate[LineCount]) + "#");
							}
						} catch (NullPointerException e) {
							ErrorOut.write("#");
						}
						if (LibraryCardNumber[LineCount] != 0) {
							ErrorOut.write(LibraryCardNumber[LineCount]);
						} else {
							ErrorOut.write(LibraryCardNumberS[LineCount]);
						}
					} else {
						try {
							if (BorrowDate[LineCount] == null) {
								ErrorOut.write(BorrowDateS[LineCount]);
							} else {
								ErrorOut.write(df.format(BorrowDate[LineCount]));
							}
						} catch (NullPointerException e) {
							
						}
					}
					ErrorOut.newLine();
		        } else {
		        	// Storing error-free lines in the database
		        	try {
		        		if (BorrowedBook[LineCount]) {
							statement.executeUpdate("INSERT IGNORE INTO books VALUES(" +
														Isbn[LineCount] + ", " +
														CopyNumber[LineCount] + ", '" +
														Title[LineCount] + "', '" +
														Author[LineCount] + "', '" +
														Publisher[LineCount] + "', " +
														Year[LineCount] + ", " +
														Statistics[LineCount] + ", " +
														df.format(BorrowDate[LineCount]) + ", " +
														df.format(ReturnDate[LineCount]) + ", " +
														LibraryCardNumber[LineCount] + ");"
													);
		        		} else {
		        			statement.executeUpdate("INSERT IGNORE INTO books VALUES(" +
														Isbn[LineCount] + ", " +
														CopyNumber[LineCount] + ", '" +
														Title[LineCount] + "', '" +
														Author[LineCount] + "', '" +
														Publisher[LineCount] + "', " +
														Year[LineCount] + ", " +
														Statistics[LineCount] + ", " +
														"000000, " +
														"000000, " +
														"0);"
		        									);
		        		}
					} catch (SQLException e) {
						JOptionPane.showMessageDialog(null, "Failed to add book " + Title[LineCount] + " to database.");
					}
		        }
		        
		        LineCount++;
		    }
		    ErrorOut.close(); // Preventing resource leaks
		    in.close(); // Preventing resource leaks
		    
		    
		    
		    //_________________________________________________________________________________________________________//
		    //																										   //
		    // 									Searching and storing of Borrowers									   //
		    //_________________________________________________________________________________________________________//
		    
		    
		    // Asking for path where you can find 'Borrowers.txt'
		    do {
				try {
					path = "";
					if (Default) {
						path = "src/";
					} else {
						path = JOptionPane.showInputDialog(null, "Please enter the path where 'Borrowers.txt' can be found.\n" +
															 	"Example: 'C:/Users/Peter/Desktop/'", "Filepath for Borrowers",
															 	JOptionPane.PLAIN_MESSAGE);
					}
					if (path == null) {
						// If the user clicks cancel in the InputDialog or closes the window, the program ends
						return;
					}
					in = new BufferedReader(new FileReader(path + "Borrowers.txt"));
					// Same procedure as with 'Books.txt'
					break;
				} catch (FileNotFoundException e) {
					JOptionPane.showMessageDialog(null, "Borrowers.txt not found. Try again.");
				}
			} while (true);
		    
		    // This procedure is also the same as with 'Books.txt',
		    // but this time there is no checking whether the line has errors or not
		    while ((CurrentLine = in.readLine()) != null) {
		    	SplitLine = CurrentLine.split("#");
		    	try {
					statement.executeUpdate("INSERT IGNORE INTO borrowers VALUES(" +
												SplitLine[0] + ", '" +	// Library Card Number
												SplitLine[1] + "', '" + // Name
												SplitLine[2] + "', " +	// Street
												SplitLine[3] + ", '" +	// Zip-code
												SplitLine[4] + "');"	// Town
											);
				} catch (SQLException e) {
					JOptionPane.showMessageDialog(null, "Failed to add boorrower " + SplitLine[1] + " to database.");
				}
		    }
		    
		    
		    
		    //_________________________________________________________________________________________________________//
		    //																										   //
		    // 									Displaying the Library Interface  									   //
		    //_________________________________________________________________________________________________________//
		    
		    
		    // The program first ask for a library card number and if the number exists in the database, the main interface opens up
			try {
				login = loginUI();
				if (login) {
					libraryUI(statement.executeQuery("SELECT Title FROM books ORDER BY Title ASC;"));
				}
			} catch (SQLException e) {
				JOptionPane.showMessageDialog(null, "Failure");
			}
		} catch (IOException e) {
			
		}
		
	}
	
	
	
			//_________________________________________________________________________________________________________//
    		//																										   //
    		// 												Methods  												   //
    		//_________________________________________________________________________________________________________//
	
	
	
	
	public static boolean loginUI() {
		String s;
		boolean found = false;
		ResultSet Res = null;
		Statement state = null;
		
		// Connecting to database
		try{
			DBConnect = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "");
			state = DBConnect.createStatement();
			state.executeUpdate("USE "+ DBName + ";");
		} catch (SQLException sqle) {
			JOptionPane.showMessageDialog(null, "Error connecting to db: " + sqle.getMessage());
			return false;
		}
		
		// Retrieving information about borrowers
		try {
			Res = state.executeQuery("SELECT * FROM borrowers;");
		} catch (SQLException e1) {
			JOptionPane.showMessageDialog(null, "Query Error");
			return false;
		}
		
		do {
			// Asking for Library card number
			do {
				try {
					s = JOptionPane.showInputDialog(null, "Please enter your library card number",
														"Library Card Number", JOptionPane.PLAIN_MESSAGE);
					if (s == null) {
						return false;
					}
					Number = Integer.parseInt(s);
					break;
				} catch (NumberFormatException ne) {
					JOptionPane.showMessageDialog(null, "Please enter a number");
				}
			} while (true);
			// Checking if library card number exists
			try {
				Res.first();
				do {
					if (Number == Res.getInt(1)) {
						BName = Res.getString(2);
						JOptionPane.showMessageDialog(null, "Welcome to the library, " + BName);
						found = true;
						break;
					}
				} while (Res.next());
				if (found == false) {
					JOptionPane.showMessageDialog(null, "Could not find the specified library card number in the database.\nTry again.");
				}
			} catch (SQLException se) {
				JOptionPane.showMessageDialog(null, "Something went wrong.");
				return false;
			}
		} while (found == false);
		return true;
	}
	
	// Method for creating the library interface
	public static void libraryUI(ResultSet ResSet) {
		
		try {
			while (ResSet.next()) {
				BookListM.addElement(ResSet.getString(1));
			}
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "Something went wrong.");
			return;
		}
		
		LibraryFrame.setSize(800, 500);
		LibraryFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		LibraryFrame.setLocation(600, 250);
		
		Container co = LibraryFrame.getContentPane();
		
		JLabel welcome = new JLabel("Welcome to the Library!");
		JLabel CurrentB = new JLabel("Current Borrower: " + BName);
		PanelN.setLayout(new BorderLayout());
		PanelN.setSize(800, 200);
		PanelN.add(welcome, BorderLayout.CENTER);
		PanelN.add(CurrentB, BorderLayout.EAST);
		co.add(PanelN, BorderLayout.NORTH);
		
		JPanel PanelS = new JPanel();
		PanelS.setLayout(new BorderLayout());
		JButton quit = new JButton("Quit");
		quit.addActionListener(new Quit());
		JButton Borrow = new JButton("Borrow Selected Books");
		Borrow.addActionListener(new BorrowBooks());
		PanelS.add(Borrow, BorderLayout.WEST);
		PanelS.add(quit, BorderLayout.EAST);
		co.add(PanelS, BorderLayout.SOUTH);
		
		JPanel PanelC = new JPanel();
		PanelC.setLayout(new BoxLayout(PanelC, BoxLayout.Y_AXIS));
		JButton Select = new JButton("--->");
		Select.addActionListener(new SelectBookButton());
		JButton Deselect = new JButton("<---");
		Deselect.addActionListener(new DeselectBookButton());
		PanelC.add(Select);
		PanelC.add(Deselect);
		co.add(PanelC, BorderLayout.CENTER);
		
		JPanel PanelW = new JPanel();
		BookList = new JList<String>(BookListM);
		pane = new JScrollPane(BookList);
		BookList.setFixedCellWidth(300);
		BookList.setVisibleRowCount(20);
		BookList.addListSelectionListener(new SelectBook());
		PanelW.add(pane);
		co.add(PanelW, BorderLayout.WEST);
		
		JPanel PanelE = new JPanel();
		BorrowList = new JList<String>(BorrowListM);
		pane2 = new JScrollPane(BorrowList);
		BorrowList.setFixedCellWidth(300);
		BorrowList.setVisibleRowCount(20);
		BorrowList.addListSelectionListener(new DeselectBook());
		PanelE.add(pane2);
		co.add(PanelE, BorderLayout.EAST);
		
		LibraryFrame.setVisible(true);
	}
	
	static class BorrowBooks implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String path;
			BufferedWriter RecieptOut = null;
			
			path = "";
			path = JOptionPane.showInputDialog(null, "Please enter the path where you want your reciept printed out.\n" +
														"Example: 'C:/Users/Peter/Desktop/'", "Filepath for Printed Reciept", 
														JOptionPane.PLAIN_MESSAGE);
			if (path == null) {
				// If the user clicks cancel in the InputDialog or closes the window, you go back to the library
				return;
			}
			try {
				RecieptOut = new BufferedWriter(new FileWriter(path + "Reciept.txt"));
				RecieptOut.write("Borrower: " + BName);
				RecieptOut.newLine();
				RecieptOut.write("Library Card Number: " + Number);
				RecieptOut.newLine();
				RecieptOut.write("Books Borrowed: " + BorrowListM);
				RecieptOut.close();
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(null, "Printing out reciept failed.");
			}
		}
	}
	
	static class SelectBookButton implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (Selection.equals("") == false) {
				BorrowListM.addElement(Selection);
				BookListM.removeElement(Selection);
				Selection = "";
			}
		}
	}
	
	static class DeselectBookButton implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (Deselection.equals("") == false) {
				BookListM.addElement(Deselection);
				BorrowListM.removeElement(Deselection);
				Deselection = "";
			}
		}
	}
	
	// When selecting a book from the list, this happens
	static class SelectBook implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event) {
			if (!event.getValueIsAdjusting()) {
				Deselection = "";
				Selection = BookList.getSelectedValue();
			}
		}
	}
	
	static class DeselectBook implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event) {
			if (!event.getValueIsAdjusting()) {
				Selection = "";
				Deselection = BorrowList.getSelectedValue();
			}
		}
	}
	
	// ActionListener for the button quit
	static class Quit implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}
	
	
	// ActionListener for the button Borrow Books
	static class LibraryCardNumber implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String s;
			int Number = 0;
			boolean found = false;
			ResultSet Res = null;
			Statement state = null;
			
			// Connecting to database
			try{
    			DBConnect = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "");
    			state = DBConnect.createStatement();
    			state.executeUpdate("USE "+ DBName + ";");
    		} catch (SQLException sqle) {
    			JOptionPane.showMessageDialog(null, "Error connecting to db: " + sqle.getMessage());
    			return;
    		}
			
			// Retrieving information about borrowers
			try {
				Res = state.executeQuery("SELECT * FROM borrowers;");
			} catch (SQLException e1) {
				JOptionPane.showMessageDialog(null, "Query Error");
				return;
			}
			
			do {
				// Asking for Library card number
				do {
					try {
						s = JOptionPane.showInputDialog(null, "Please enter your library card number",
															"Library Card Number", JOptionPane.PLAIN_MESSAGE);
						if (s == null) {
							return;
						}
						Number = Integer.parseInt(s);
						break;
					} catch (NumberFormatException ne) {
						JOptionPane.showMessageDialog(null, "Please enter a number");
					}
				} while (true);
				// Checking if library card number exists
				try {
					Res.first();
					do {
						if (Number == Res.getInt(1)) {
							JOptionPane.showMessageDialog(null, "Welcome, " + Res.getString(2));
							found = true;
							pane.setVisible(true);
							break;
						}
					} while (Res.next());
					if (found == false) {
						JOptionPane.showMessageDialog(null, "Could not find the specified library card number in the database.\nTry again.");
					}
				} catch (SQLException se) {
					JOptionPane.showMessageDialog(null, "Something went wrong.");
					return;
				}
			} while (found == false);
		}
	}
	
	// Method for checking the validity of an Isbn-number
	public static boolean isISBN13Valid(String isbn) {
	    int check = 0;
	    for (int i = 0; i < 12; i += 2) {
	        check += Integer.valueOf(isbn.substring(i, i + 1));
	    }
	    for (int i = 1; i < 12; i += 2) {
	        check += Integer.valueOf(isbn.substring(i, i + 1)) * 3;
	    }
	    check += Integer.valueOf(isbn.substring(12));
	    return check % 10 == 0;
	}
}
    		