package open.dolphin.client;

import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import open.dolphin.project.Project;

/**
 *
 * @author kazushi Minagawa. Digital Globe, Inc.
 */
public class KartePanel2M extends Panel2 {
    
    private static final int PREFERED_WIDTH  = 340;
    private static final int PREFERED_HEIGHT = 600;
    private static final int MAX_HEIGHT      = 4096;
    
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextPane pTextPane;
    private javax.swing.JTextPane soaTextPane;
    private javax.swing.JLabel timeStampLabel;
    private int fontSize;

    public KartePanel2M() {
        fontSize = Project.getInt("karte.font.size.default");
        initComponents();
    }

    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        timeStampLabel = new javax.swing.JLabel();
        soaTextPane = new javax.swing.JTextPane();
        pTextPane = new javax.swing.JTextPane();
//masuda^ jdk7 wrap problem 2013/06/06
        soaTextPane.setEditorKit(new WrapEditorKit());
        pTextPane.setEditorKit(new WrapEditorKit());
//masuda$        
        setAutoscrolls(true);

        jPanel2.setMaximumSize(new java.awt.Dimension(55, 26));

        timeStampLabel.setText("");
        jPanel2.add(timeStampLabel);

        //soaTextPane.setFont(new java.awt.Font("SansSerif", 0, fontSize));
        soaTextPane.setFont(new java.awt.Font("Lucida Grande", 0, fontSize));
        soaTextPane.setMargin(new java.awt.Insets(10, 10, 10, 10));
        soaTextPane.setPreferredSize(new java.awt.Dimension(PREFERED_WIDTH, PREFERED_HEIGHT));

        //pTextPane.setFont(new java.awt.Font("SansSerif", 0, fontSize));
        pTextPane.setFont(new java.awt.Font("Lucida Grande", 0, fontSize));
        pTextPane.setMargin(new java.awt.Insets(10, 10, 10, 10));
        pTextPane.setPreferredSize(new java.awt.Dimension(PREFERED_WIDTH, PREFERED_HEIGHT));

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(soaTextPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, PREFERED_WIDTH, Short.MAX_VALUE)
                .add(2, 2, 2)
                .add(pTextPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, PREFERED_WIDTH, Short.MAX_VALUE))
            .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, PREFERED_WIDTH*2+2, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(pTextPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, MAX_HEIGHT, Short.MAX_VALUE)
                    .add(soaTextPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, MAX_HEIGHT, Short.MAX_VALUE)))
        );
    }
    
//masuda^ jdk7 wrap 2013/06/06
    class WrapEditorKit extends StyledEditorKit {
        ViewFactory defaultFactory=new WrapColumnFactory();
        @Override
        public ViewFactory getViewFactory() {
            return defaultFactory;
        }
    }
 
    class WrapColumnFactory implements ViewFactory {
        @Override
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new WrapLabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new ParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }
 
            // default to text display
            return new LabelView(elem);
        }
    }
 
    class WrapLabelView extends LabelView {
        public WrapLabelView(Element elem) {
            super(elem);
        }
 
        public float getMinimumSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    return 0;
                case View.Y_AXIS:
                    return super.getMinimumSpan(axis);
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }
    }
//masuda^ jdk7 wrap          
    public javax.swing.JTextPane getPTextPane() {
        return pTextPane;
    }

    public javax.swing.JTextPane getSoaTextPane() {
        return soaTextPane;
    }

    public javax.swing.JLabel getTimeStampLabel() {
        return timeStampLabel;
    }
    
     public javax.swing.JPanel getTimeStampPanel() {
        return jPanel2;
    }
}
