package RI.practica2_2;

import static org.apache.commons.math3.util.Precision.round;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

public class SearchGUI {

  private final CustomIndex index;
  private JFrame frame;
  private JTextField searchField;
  private JTextPane resultPane;
  private StyledDocument doc;

  public SearchGUI() throws TikaException, IOException, SAXException {
    initialize();
    index = new CustomIndex();
  }

  public static void main(String[] args) {
    EventQueue.invokeLater(() -> {
      try {
        SearchGUI window = new SearchGUI();
        window.frame.setVisible(true);
      } catch (Exception e) {
        e.printStackTrace(System.out);
      }
    });
  }

  private void initialize() {
    frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setTitle("Práctica 2.2");
    frame.setSize(600, 400);

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));
    searchField = new JTextField();

    searchField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (searchField.getText().isEmpty() && (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
            || e.getKeyCode() == KeyEvent.VK_DELETE)) {
          e.consume();
        }
      }
    });

    JButton searchButton = new JButton("Search");
    ActionListener searchAction = e -> performSearch();
    searchButton.addActionListener(searchAction);

    searchField.addActionListener(searchAction);

    topPanel.add(searchField);
    topPanel.add(searchButton);

    resultPane = new JTextPane();
    resultPane.setEditable(false);

    DefaultCaret caret = (DefaultCaret) resultPane.getCaret();
    caret.setBlinkRate(0);
    resultPane.setCaret(new DefaultCaret() {
      @Override
      public void paint(Graphics g) {
      }
    });

    resultPane.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        if (isMouseOverText(e)) {
          resultPane.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        } else {
          resultPane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
      }
    });

    doc = resultPane.getStyledDocument();
    JScrollPane scrollPane = new JScrollPane(resultPane);

    frame.getContentPane().add(topPanel, BorderLayout.NORTH);
    frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
  }

  private boolean isMouseOverText(MouseEvent e) {
    JTextComponent component = (JTextComponent) e.getSource();
    int pos = component.viewToModel2D(e.getPoint());

    if (pos < 0) {
      return false;
    }

    Document doc = component.getDocument();
    int docLength = doc.getLength();

    try {
      Rectangle2D r = component.modelToView2D(pos);
      if (r == null) {
        return false;
      }

      int startPos = Math.max(0, pos - 2);
      int endPos = Math.min(docLength, pos + 1);

      Rectangle2D startR = component.modelToView2D(startPos);
      Rectangle2D endR = component.modelToView2D(endPos);

      if (startR != null && endR != null) {
        Rectangle2D expandedRect = new Rectangle2D.Double(startR.getX(), startR.getY(),
            endR.getX() - startR.getX() + endR.getWidth(),
            Math.max(startR.getHeight(), endR.getHeight()));

        if (expandedRect.contains(e.getPoint())) {
          return true;
        }
      }
    } catch (BadLocationException ex) {
      ex.printStackTrace(System.out);
    }
    return false;
  }

  private void performSearch() {
    String query = searchField.getText();
    try {
      List<DocumentRank> results = index.searchIndex(query);
      doc.remove(0, doc.getLength());

      if (results.size() >= 2) {
        DocumentRank topDoc = results.get(0);

        boolean shouldHighlightTopDoc = topDoc.score() >= 1.25 * results.get(1).score();
        String highlightText =
            shouldHighlightTopDoc ? "Best recommendation:\n" : "";
        String highlightLineJump = shouldHighlightTopDoc ? "\n" : "";
        String otherResultsHighlight = shouldHighlightTopDoc ? "Other results:\n" : "";

        addTextWithStyle(
            highlightText + topDoc.title() + " - Score: " + round(topDoc.score(),
                3) + "\n" + highlightLineJump, shouldHighlightTopDoc);

        addTextWithStyle(otherResultsHighlight, false);

        for (int i = 1; i < results.size(); i++) {
          DocumentRank doc = results.get(i);
          addTextWithStyle(doc.title() + " - Score: " + round(doc.score(),
              3) + "\n", false);
        }
      } else if (results.size() == 1) {
        DocumentRank topDoc = results.get(0);
        addTextWithStyle(topDoc.title() + " - Score: " + round(topDoc.score(),
                3) + "\n", true);
      }
    } catch (Exception e) {
      addTextWithStyle("Error occurred: " + e.getMessage(), false);
    }
  }

  private void addTextWithStyle(String text, boolean isBold) {
    Style style = resultPane.addStyle("Style", null);
    StyleConstants.setBold(style, isBold);

    try {
      doc.insertString(doc.getLength(), text, style);
    } catch (BadLocationException e) {
      e.printStackTrace(System.out);
    }
  }
}
