package com.mybank.gui;

import com.mybank.data.DataSource;
import com.mybank.domain.Bank;
import com.mybank.domain.CheckingAccount;
import com.mybank.domain.Customer;
import com.mybank.reporting.CustomerReport;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Locale;
public class Main {

    private final JEditorPane log;
    private final JButton show;
    private final JButton report;
    private final JComboBox<String> clients;
    private final NumberFormat currencyFormat;

    public Main() {
        log = new JEditorPane("text/html", "");
        log.setPreferredSize(new Dimension(400, 450));
        log.setEditable(false);
        log.setBackground(Color.WHITE);
        log.setBorder(new EmptyBorder(10, 10, 10, 10));

        show = new JButton("Show");
        show.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        show.setForeground(Color.BLACK); // Black text
        show.setFocusPainted(false);

        report = new JButton("Report");
        report.setFont(new Font("Arial", Font.BOLD, 14));
        report.setForeground(Color.BLACK);
        report.setBackground(new Color(70, 130, 180));
        report.setFocusPainted(false);
        report.setBorder(BorderFactory.createLineBorder(new Color(173, 216, 230), 2));

        report.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                report.setBackground(new Color(173, 216, 230));
                report.setForeground(Color.BLACK);
                report.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                report.setBackground(new Color(70, 130, 180));
                report.setForeground(Color.BLACK);
                report.setCursor(Cursor.getDefaultCursor());
            }
        });

        clients = new JComboBox<>();
        clients.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "US"));

        loadCustomerList();
    }

    private void loadCustomerList() {
        clients.removeAllItems();
        for (int i = 0; i < Bank.getNumberOfCustomers(); i++) {
            Customer customer = Bank.getCustomer(i);
            String customerName = customer.getLastName() + ", " + customer.getFirstName();
            clients.addItem(customerName);
        }
    }

    private void launchFrame() {
        JFrame frame = new JFrame("MyBank clients");
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(49, 106, 197));
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topPanel.setPreferredSize(new Dimension(400, 45));

        clients.setPreferredSize(new Dimension(120, 25));
        show.setPreferredSize(new Dimension(60, 25));

        report.setPreferredSize(new Dimension(70, 25));
        report.setFont(new Font("Segoe UI", Font.BOLD, 11));

        topPanel.add(clients);
        topPanel.add(show);
        topPanel.add(report);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(log), BorderLayout.CENTER);

        setupEventHandlers();

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    private void setupEventHandlers() {
        show.addActionListener(e -> {
            int selectedIndex = clients.getSelectedIndex();
            if (selectedIndex >= 0) {
                displayCustomerInfo(selectedIndex);
            }
        });

        report.addActionListener(e -> generateReport());

        clients.addActionListener(e -> {
            if (clients.getSelectedIndex() >= 0) {
                displayCustomerInfo(clients.getSelectedIndex());
            }
        });
    }

    private void displayCustomerInfo(int customerIndex) {
        Customer current = Bank.getCustomer(customerIndex);
        String accType = current.getAccount(0) instanceof CheckingAccount ? "Checking" : "Savings";

        StringBuilder info = new StringBuilder();
        info.append(current.getLastName()).append(", ").append(current.getFirstName()).append("\n");
        info.append("_".repeat(50)).append("\n\n");

        info.append("Account:\t\t#").append(customerIndex).append("\n");
        info.append("Acc Type:\t\t").append(accType).append("\n");
        info.append("Balance:\t\t").append(currencyFormat.format(current.getAccount(0).getBalance()));

        String htmlContent = "<div style='font-family: Segoe UI, Arial, sans-serif; font-size: 12px; margin: 10px;'>" +
                "<pre>" + info.toString() + "</pre></div>";

        log.setText(htmlContent);
    }

    private void generateReport() {
        try {
            String reportContent = captureReportOutput();
            String formattedReport = formatReportForDisplay(reportContent);
            log.setText(formattedReport);
        } catch (Exception ex) {
            String errorMsg = "<div style='color: red; font-weight: bold; margin: 10px;'>" +
                    "Error generating report: " + ex.getMessage() + "</div>";
            log.setText(errorMsg);
        }
    }

    private String captureReportOutput() {
        PrintStream originalOut = System.out;

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             PrintStream printStream = new PrintStream(byteStream, true, "UTF-8")) {

            System.setOut(printStream);

            CustomerReport customerReport = new CustomerReport();
            customerReport.generateReport();

            return byteStream.toString("UTF-8");

        } catch (Exception ex) {
            throw new RuntimeException("Error creating report", ex);
        } finally {
            System.setOut(originalOut);
        }
    }

    private String formatReportForDisplay(String reportContent) {
        String[] lines = reportContent.split("\n");

        // Skip first two lines (header) and format the rest
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<div style='font-family: monospace; font-size: 12px; margin: 10px;'>");

        for (int i = 2; i < lines.length; i++) {
            htmlContent.append(lines[i]).append("<br>");
        }

        htmlContent.append("</div>");
        return htmlContent.toString();
    }

    public static void main(String[] args) {
        Locale.setDefault(new Locale("en", "US"));

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {

        }

        try {
            new DataSource("./data/test.dat").loadData();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Could not load customer data: " + e.getMessage(),
                    "Data Loading Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            Main demo = new Main();
            demo.launchFrame();
        });
    }
}