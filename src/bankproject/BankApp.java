package bankproject;

import java.sql.*;
import java.util.Scanner;

public class BankApp {

	    private static Connection con;
	    private static final Scanner sc = new Scanner(System.in);

	    public static void main(String[] args) {
	        try {
	            // Load JDBC driver
	            Class.forName("com.mysql.cj.jdbc.Driver");

	            // Connect to DB - update user name/password if different
	            con = DriverManager.getConnection(
	                    "jdbc:mysql://localhost:3306/bankdb?serverTimezone=UTC",
	                    "root",
	                    "sanjana"
	            );

	            System.out.println("Connected to bankdb.");

	            // Login first
	            if (!login()) {
	                System.out.println("Invalid login. Exiting.");
	                return;
	            }

	            // Menu loop
	            while (true) {
	                System.out.println("\n===== BANK MENU =====");
	                System.out.println("1. Create Account");
	                System.out.println("2. Deposit");
	                System.out.println("3. Withdraw");
	                System.out.println("4. Check Balance");
	                System.out.println("5. Money Transfer");
	                System.out.println("6. Delete Account");
	                System.out.println("7. Update Account Details");
	                System.out.println("8. Mini Statement");
	                System.out.println("9. Exit");
	                System.out.print("Enter choice: ");

	                int ch = safeNextInt();
	                switch (ch) {
	                    case 1 -> createAccount();
	                    case 2 -> deposit();
	                    case 3 -> withdraw();
	                    case 4 -> checkBalance();
	                    case 5 -> moneyTransfer();
	                    case 6 -> deleteAccount();
	                    case 7 -> updateAccount();
	                    case 8 -> miniStatement();
	                    case 9 -> {
	                        System.out.println("Goodbye!");
	                        return;
	                    }
	                    default -> System.out.println("Invalid choice. Try again.");
	                }
	            }

	        } catch (ClassNotFoundException e) {
	            System.out.println("JDBC Driver not found. Add the MySQL connector JAR to classpath.");
	            e.printStackTrace();
	        } catch (SQLException e) {
	            System.out.println("Database error:");
	            e.printStackTrace();
	        } finally {
	            // close connection
	            if (con != null) {
	                try {
	                    con.close();
	                    // System.out.println("Connection closed.");
	                } catch (SQLException ignored) {}
	            }
	            sc.close();
	        }
	    }

	    // -----------------------
	    // Utility to read int safely (avoids InputMismatchException)
	    private static int safeNextInt() {
	        while (true) {
	            try {
	                return Integer.parseInt(sc.next());
	            } catch (NumberFormatException e) {
	                System.out.print("Please enter a valid integer: ");
	            }
	        }
	    }

	    // -----------------------
	    // LOGIN
	    private static boolean login() {
	        System.out.print("Login - Enter username: ");
	        String user = sc.next().trim();
	        System.out.print("Enter password: ");
	        String pass = sc.next().trim();

	        String q = "SELECT username FROM users WHERE username=? AND password=?";
	        try (PreparedStatement ps = con.prepareStatement(q)) {
	            ps.setString(1, user);
	            ps.setString(2, pass);
	            try (ResultSet rs = ps.executeQuery()) {
	                return rs.next();
	            }
	        } catch (SQLException e) {
	            System.out.println("Login failed due to DB error.");
	            e.printStackTrace();
	            return false;
	        }
	    }

	    // -----------------------
	    // CREATE ACCOUNT + create user login
	    private static void createAccount() {
	        try {
	            System.out.print("Enter Account No: ");
	            int acc = safeNextInt();

	            System.out.print("Enter Name: ");
	            String name = sc.next();

	            System.out.print("Enter Address: ");
	            String address = sc.next();

	            System.out.print("Enter Initial Balance: ");
	            double bal = Double.parseDouble(sc.next());

	            String q = "INSERT INTO accounts(acc_no, name, address, balance) VALUES (?, ?, ?, ?)";
	            try (PreparedStatement ps = con.prepareStatement(q)) {
	                ps.setInt(1, acc);
	                ps.setString(2, name);
	                ps.setString(3, address);
	                ps.setDouble(4, bal);
	                ps.executeUpdate();
	            }

	            System.out.println("Account created.");

	            // create login for this account
	            System.out.print("Set username for this account: ");
	            String u = sc.next();
	            System.out.print("Set password: ");
	            String p = sc.next();

	            String q2 = "INSERT INTO users(username, password, acc_no) VALUES (?, ?, ?)";
	            try (PreparedStatement ps2 = con.prepareStatement(q2)) {
	                ps2.setString(1, u);
	                ps2.setString(2, p);
	                ps2.setInt(3, acc);
	                ps2.executeUpdate();
	            }

	            System.out.println("User login created for account " + acc);

	        } catch (SQLException e) {
	            System.out.println("Error creating account. Check if account number or username already exists.");
	            e.printStackTrace();
	        }
	    }

	    // -----------------------
	    // DEPOSIT
	    private static void deposit() {
	        try {
	            System.out.print("Enter Account No: ");
	            int acc = safeNextInt();
	            System.out.print("Enter Amount to deposit: ");
	            double amt = Double.parseDouble(sc.next());

	            String q = "UPDATE accounts SET balance = balance + ? WHERE acc_no = ?";
	            try (PreparedStatement ps = con.prepareStatement(q)) {
	                ps.setDouble(1, amt);
	                ps.setInt(2, acc);
	                int rows = ps.executeUpdate();
	                if (rows > 0) {
	                    saveTransaction(acc, "deposit", amt);
	                    System.out.println("Deposited ₹" + amt + " to account " + acc);
	                } else {
	                    System.out.println("Account not found.");
	                }
	            }
	        } catch (SQLException e) {
	            System.out.println("Deposit failed.");
	            e.printStackTrace();
	        }
	    }

	    // -----------------------
	    // WITHDRAW
	    private static void withdraw() {
	        try {
	            System.out.print("Enter Account No: ");
	            int acc = safeNextInt();
	            System.out.print("Enter Amount to withdraw: ");
	            double amt = Double.parseDouble(sc.next());

	            String q1 = "SELECT balance FROM accounts WHERE acc_no = ?";
	            try (PreparedStatement ps1 = con.prepareStatement(q1)) {
	                ps1.setInt(1, acc);
	                try (ResultSet rs = ps1.executeQuery()) {
	                    if (rs.next()) {
	                        double bal = rs.getDouble(1);
	                        if (bal >= amt) {
	                            String q2 = "UPDATE accounts SET balance = balance - ? WHERE acc_no = ?";
	                            try (PreparedStatement ps2 = con.prepareStatement(q2)) {
	                                ps2.setDouble(1, amt);
	                                ps2.setInt(2, acc);
	                                ps2.executeUpdate();
	                            }
	                            saveTransaction(acc, "withdraw", amt);
	                            System.out.println("Withdrawn ₹" + amt + " from account " + acc);
	                        } else {
	                            System.out.println("Insufficient balance. Current balance: ₹" + bal);
	                        }
	                    } else {
	                        System.out.println("Account not found.");
	                    }
	                }
	            }
	        } catch (SQLException e) {
	            System.out.println("Withdraw failed.");
	            e.printStackTrace();
	        }
	    }

	    // -----------------------
	    // CHECK BALANCE
	    private static void checkBalance() {
	        try {
	            System.out.print("Enter Account No: ");
	            int acc = safeNextInt();

	            String q = "SELECT name, balance FROM accounts WHERE acc_no = ?";
	            try (PreparedStatement ps = con.prepareStatement(q)) {
	                ps.setInt(1, acc);
	                try (ResultSet rs = ps.executeQuery()) {
	                    if (rs.next()) {
	                        System.out.println("Name   : " + rs.getString("name"));
	                        System.out.println("Balance: ₹" + rs.getDouble("balance"));
	                    } else {
	                        System.out.println("Account not found.");
	                    }
	                }
	            }
	        } catch (SQLException e) {
	            System.out.println("Check balance failed.");
	            e.printStackTrace();
	        }
	    }

	    // -----------------------
	    // MONEY TRANSFER (from -> to)
	    private static void moneyTransfer() {
	        try {
	            System.out.print("Enter Sender Account No: ");
	            int from = safeNextInt();
	            System.out.print("Enter Receiver Account No: ");
	            int to = safeNextInt();
	            System.out.print("Enter Amount to transfer: ");
	            double amt = Double.parseDouble(sc.next());

	            // Check sender balance
	            String q1 = "SELECT balance FROM accounts WHERE acc_no = ?";
	            try (PreparedStatement ps1 = con.prepareStatement(q1)) {
	                ps1.setInt(1, from);
	                try (ResultSet rs = ps1.executeQuery()) {
	                    if (rs.next()) {
	                        double bal = rs.getDouble(1);
	                        if (bal >= amt) {
	                            // Deduct from sender
	                            try (PreparedStatement ps2 = con.prepareStatement(
	                                    "UPDATE accounts SET balance = balance - ? WHERE acc_no = ?")) {
	                                ps2.setDouble(1, amt);
	                                ps2.setInt(2, from);
	                                ps2.executeUpdate();
	                            }
	                            // Add to receiver
	                            try (PreparedStatement ps3 = con.prepareStatement(
	                                    "UPDATE accounts SET balance = balance + ? WHERE acc_no = ?")) {
	                                ps3.setDouble(1, amt);
	                                ps3.setInt(2, to);
	                                int rows = ps3.executeUpdate();
	                                if (rows == 0) {
	                                    // receiver not found -> rollback sender update
	                                    try (PreparedStatement psRollback = con.prepareStatement(
	                                            "UPDATE accounts SET balance = balance + ? WHERE acc_no = ?")) {
	                                        psRollback.setDouble(1, amt);
	                                        psRollback.setInt(2, from);
	                                        psRollback.executeUpdate();
	                                    }
	                                    System.out.println("Receiver account not found. Transfer cancelled and rolled back.");
	                                    return;
	                                }
	                            }
	                            // Save transactions for both accounts
	                            saveTransaction(from, "transfer_out", amt);
	                            saveTransaction(to, "transfer_in", amt);
	                            System.out.println("Transferred ₹" + amt + " from " + from + " to " + to);
	                        } else {
	                            System.out.println("Insufficient balance in sender account. Current balance: ₹" + bal);
	                        }
	                    } else {
	                        System.out.println("Sender account not found.");
	                    }
	                }
	            }
	        } catch (SQLException e) {
	            System.out.println("Transfer failed.");
	            e.printStackTrace();
	        }
	    }

	    // -----------------------
	    // DELETE ACCOUNT (removes user login too)
	    private static void deleteAccount() {
	        try {
	            System.out.print("Enter Account No to delete: ");
	            int acc = safeNextInt();

	            // delete user mapping(s)
	            try (PreparedStatement ps1 = con.prepareStatement("DELETE FROM users WHERE acc_no = ?")) {
	                ps1.setInt(1, acc);
	                ps1.executeUpdate();
	            }

	            // delete account
	            try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM accounts WHERE acc_no = ?")) {
	                ps2.setInt(1, acc);
	                int rows = ps2.executeUpdate();
	                if (rows > 0) {
	                    System.out.println("Account " + acc + " deleted.");
	                } else {
	                    System.out.println("Account not found.");
	                }
	            }
	        } catch (SQLException e) {
	            System.out.println("Delete failed.");
	            e.printStackTrace();
	        }
	    }

	    // -----------------------
	    // UPDATE ACCOUNT DETAILS (name, address)
	    private static void updateAccount() {
	        try {
	            System.out.print("Enter Account No to update: ");
	            int acc = safeNextInt();

	            System.out.print("Enter new name: ");
	            String name = sc.next();

	            System.out.print("Enter new address: ");
	            String address = sc.next();

	            try (PreparedStatement ps = con.prepareStatement(
	                    "UPDATE accounts SET name = ?, address = ? WHERE acc_no = ?")) {
	                ps.setString(1, name);
	                ps.setString(2, address);
	                ps.setInt(3, acc);
	                int rows = ps.executeUpdate();
	                if (rows > 0) {
	                    System.out.println("Account updated.");
	                } else {
	                    System.out.println("Account not found.");
	                }
	            }
	        } catch (SQLException e) {
	            System.out.println("Update failed.");
	            e.printStackTrace();
	        }
	    }

	    // -----------------------
	    // MINI STATEMENT (last 5 transactions)
	    private static void miniStatement() {
	        try {
	            System.out.print("Enter Account No for mini statement: ");
	            int acc = safeNextInt();

	            String q = "SELECT tid, type, amount, date_time FROM transactions WHERE acc_no = ? ORDER BY date_time DESC LIMIT 5";
	            try (PreparedStatement ps = con.prepareStatement(q)) {
	                ps.setInt(1, acc);
	                try (ResultSet rs = ps.executeQuery()) {
	                    System.out.println("\n--- Mini Statement (latest 5) ---");
	                    boolean any = false;
	                    while (rs.next()) {
	                        any = true;
	                        System.out.printf("%d | %s | ₹%.2f | %s%n",
	                                rs.getInt("tid"),
	                                rs.getString("type"),
	                                rs.getDouble("amount"),
	                                rs.getTimestamp("date_time").toString());
	                    }
	                    if (!any) System.out.println("No transactions found for account " + acc);
	                }
	            }
	        } catch (SQLException e) {
	            System.out.println("Mini statement failed.");
	            e.printStackTrace();
	        }
	    }

	    // -----------------------
	    // SAVE TRANSACTION
	    private static void saveTransaction(int acc, String type, double amt) {
	        String q = "INSERT INTO transactions(acc_no, type, amount) VALUES (?, ?, ?)";
	        try (PreparedStatement ps = con.prepareStatement(q)) {
	            ps.setInt(1, acc);
	            ps.setString(2, type);
	            ps.setDouble(3, amt);
	            ps.executeUpdate();
	        } catch (SQLException e) {
	            System.out.println("Warning: failed to save transaction for account " + acc);
	            e.printStackTrace();
	        }
	    }
	}