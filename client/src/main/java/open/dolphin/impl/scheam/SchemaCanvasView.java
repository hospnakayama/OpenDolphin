package open.dolphin.impl.scheam;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import open.dolphin.impl.scheam.schemahelper.SchemaTitleBorder;

/**
 *
 * @author  pns
 */
public class SchemaCanvasView extends javax.swing.JFrame {

    private SchemaToolView toolView;
    private SchemaCanvasView canvasView;

    public SchemaCanvasView() {
        initComponents();
        initTitlePanel();

        // toolView が後ろに行った場合，canvas をクリックすると toFront されるようにする
        this.getRootPane().addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                toolView.toFront();
            }
        });
    }

    /**
     * JFrame を undecorated にして
     * titlePanel をつかんで移動できるようにする
     */
    private void initTitlePanel() {
        canvasView = this;
        TitlePanelListener l = new TitlePanelListener();
        titlePanel.addMouseListener(l);
        titlePanel.addMouseMotionListener(l);
        titlePanel.setBorder(new SchemaTitleBorder());
        titleLabel.setText(SchemaEditorImpl.TITLE);
    }

    /**
     * toolView に近づいたらくっつける，くっついたら一緒に動く
     */
    private class TitlePanelListener extends MouseAdapter implements Runnable {
        private Point from;
        private int THRESHOLD = 16;
        private boolean attached;

        @Override
        public void mousePressed(MouseEvent e) {
            toolView.toFront();
            from = e.getLocationOnScreen();
            attached = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point to = e.getLocationOnScreen();
            
            int dx = to.x - from.x;
            int dy = to.y - from.y;

            Rectangle toolRect = toolView.getBounds();
            Rectangle canvasRect = canvasView.getBounds();
            int dxTcL = toolRect.x + toolRect.width - canvasRect.x;
            int dxTcR = canvasRect.x + canvasRect.width - toolRect.x;

            // 近づいたらくっつける動作
            if ((-THRESHOLD < dxTcL && dxTcL < THRESHOLD) || (-THRESHOLD < dxTcR && dxTcR < THRESHOLD)) {
                attached = true;
            }
            canvasView.setBounds(canvasRect.x + dx, canvasRect.y + dy, canvasRect.width, canvasRect.height);

            new Thread(this).start();
            
            from.x = to.x; from.y = to.y;
        }
        
        @Override
        public void run() {
            Rectangle toolRect = toolView.getBounds();
            Rectangle canvasRect = canvasView.getBounds();
            if (attached) {
                if (toolRect.x < canvasRect.x) {
                    // toolView が左にある場合
                    toolRect.x = canvasRect.x - toolRect.width - 2;
                } else {
                    // toolView が右にある場合
                    toolRect.x = canvasRect.x + canvasRect.width + 2;
                }
                toolView.setBounds(toolRect.x, canvasRect.y, toolRect.width, toolRect.height);
            }
        }
    }

    public void setSchemaToolView(SchemaToolView view) {
        toolView = view;
    }

    public javax.swing.JPanel getTitlePanel() {
        return titlePanel;
    }

    public javax.swing.JButton getCancelBtn() {
        return cancelBtn;
    }

    public javax.swing.JPanel getCanvasPanel() {
        return canvasPanel;
    }

    public javax.swing.JButton getOkBtn() {
        return okBtn;
    }

    public javax.swing.JComboBox getRoleCombo() {
        return roleCombo;
    }

    public javax.swing.JTextField getTitleFld() {
        return titleFld;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        canvasPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        titleFld = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        roleCombo = new javax.swing.JComboBox();
        okBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        titlePanel = new javax.swing.JPanel();
        titleLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        setUndecorated(true);

        canvasPanel.setBackground(new java.awt.Color(255, 255, 255));
        canvasPanel.setPreferredSize(new java.awt.Dimension(100, 150));

        jLabel1.setText("タイトル:");

        jLabel2.setText("用途:");

        roleCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "検体検査", "内視鏡検査", "単純レントゲン写真", "上部消化管造影検査", "バリウム注腸検査", "CTスキャン注腸検査", "MRI", "R画像検査", "血管造影", "その他の放射線学的検査", "エコー", "心電図", "脳波", "筋電図", "心電図", "肺機能検査", "その他の生理学的検査", "処方箋", "熱型表", "理学的所見（図など）", "麻酔経過表", "病理検査（画像など）", "手術記録", "参考文献", "参考図", "処置（指示、記録など）", "上記に含まれないもの" }));

        okBtn.setText("カルテに展開");
        okBtn.setSelected(true);

        cancelBtn.setText("破棄");

        titleLabel.setFont(new java.awt.Font("Lucida Grande", 0, 9)); // NOI18N
        titleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        titleLabel.setText("シェーマエディタ");
        titleLabel.setMaximumSize(new java.awt.Dimension(10000, 16));
        titleLabel.setMinimumSize(new java.awt.Dimension(72, 16));
        titleLabel.setPreferredSize(new java.awt.Dimension(72, 16));

        javax.swing.GroupLayout titlePanelLayout = new javax.swing.GroupLayout(titlePanel);
        titlePanel.setLayout(titlePanelLayout);
        titlePanelLayout.setHorizontalGroup(
            titlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(titleLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 562, Short.MAX_VALUE)
        );
        titlePanelLayout.setVerticalGroup(
            titlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(titleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(titlePanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(titleFld, javax.swing.GroupLayout.PREFERRED_SIZE, 456, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(roleCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 56, Short.MAX_VALUE)
                .addComponent(cancelBtn)
                .addGap(18, 18, 18)
                .addComponent(okBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(canvasPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(titlePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(titleFld, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(canvasPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 367, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(roleCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(okBtn)
                    .addComponent(cancelBtn)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JPanel canvasPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton okBtn;
    private javax.swing.JComboBox roleCombo;
    private javax.swing.JTextField titleFld;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JPanel titlePanel;
    // End of variables declaration//GEN-END:variables
}
