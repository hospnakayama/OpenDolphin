/*
 * Created on 2005/06/01
 *
 */
package open.dolphin.client;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentListener;

import open.dolphin.project.ProjectStub;

/**
 * @author Kazushi Minagawa Digital Globe, Inc.
 *
 */
public class SendMmlSettingPanel extends AbstractSettingPanel {
    
    // MML���M�֌W�R���|�[�l���g
    private JRadioButton sendMML;
    private JRadioButton sendNoMML;
    private JRadioButton mml3;
    private JRadioButton mml23;
    private JTextField uploaderServer;
    private JTextField shareDirectory;
    private JComboBox protocolCombo;
    
    private MmlModel model;
    
    private StateMgr stateMgr;
    
    public SendMmlSettingPanel() {
    }
    
    /**
     * MML�o�͂��J�n����B
     */
    public void start() {
        
        //
        // ���f���𐶐�����
        //
        model = new MmlModel();
        
        //
        // GUI
        //
        initComponents();
        
        //
        // populate
        //
        model.populate(getProjectStub());
        
    }
    
    /**
     * �ۑ�����B
     */
    public void save() {
        model.restore(getProjectStub());
    }
    
    /**
     * GUI���\�z����B
     */
    private void initComponents() {
        
        // ����
        ButtonGroup bg = new ButtonGroup();
        sendMML = GUIFactory.createRadioButton("����", null, bg);
        sendNoMML = GUIFactory.createRadioButton("���Ȃ�", null, bg);
        bg = new ButtonGroup();
        mml3 = GUIFactory.createRadioButton("3.0", null, bg);
        mml23 = GUIFactory.createRadioButton("2.3", null, bg);
        uploaderServer = GUIFactory.createTextField(10, null, null, null);
        shareDirectory = GUIFactory.createTextField(10, null, null, null);
        protocolCombo = new JComboBox(new String[]{"Samba"});
        
        // ���C�A�E�g
        
        GridBagBuilder gbl = new GridBagBuilder("MML(XML)�o��");
        gbl.add(GUIFactory.createRadioPanel(new JRadioButton[]{sendMML,sendNoMML}), 0, 0, 2, 1, GridBagConstraints.CENTER);
        gbl.add(new JLabel("MML �o�[�W����:", SwingConstants.RIGHT),	0, 1, 1, 1, GridBagConstraints.EAST);
        gbl.add(GUIFactory.createRadioPanel(new JRadioButton[]{mml23,mml3}), 1, 1, 1, 1, GridBagConstraints.WEST);
        gbl.add(new JLabel("���M�v���g�R��:", SwingConstants.RIGHT), 0, 2, 1, 1, GridBagConstraints.EAST);
        gbl.add(protocolCombo,	1, 2, 1, 1, GridBagConstraints.WEST);
        gbl.add(new JLabel("���M�T�[�o�A�h���X:", SwingConstants.RIGHT),     0, 3, 1, 1, GridBagConstraints.EAST);
        gbl.add(uploaderServer,                                           1, 3, 1, 1, GridBagConstraints.WEST);
        gbl.add(new JLabel("���M��f�B���N�g��:", SwingConstants.RIGHT),     0, 4, 1, 1, GridBagConstraints.EAST);
        gbl.add(shareDirectory,                                           1, 4, 1, 1, GridBagConstraints.WEST);
        JPanel content = gbl.getProduct();
        
        // �S�̂����C�A�E�g����
        gbl = new GridBagBuilder();
        gbl.add(content,        0, 0, GridBagConstraints.HORIZONTAL, 1.0, 0.0);
        gbl.add(new JLabel(""), 0, 1, GridBagConstraints.BOTH,       1.0, 1.0);
        
        setUI(gbl.getProduct());
        
    }
    
    public void connect() {
        
        stateMgr = new StateMgr();
        
        // MML���M�{�^�����N���b�N���ꂽ�� State check ���s��
        ActionListener al = ProxyActionListener.create(stateMgr, "controlSendMml");
        sendMML.addActionListener(al);
        sendNoMML.addActionListener(al);
        
        // �e�L�X�g�t�B�[���h�̃C�x���g����������@State check ���s��
        DocumentListener dl = ProxyDocumentListener.create(stateMgr, "checkState");
        uploaderServer.getDocument().addDocumentListener(dl);
        shareDirectory.getDocument().addDocumentListener(dl);
        
        //
        // IME OFF FocusAdapter
        //
        uploaderServer.addFocusListener(AutoRomanListener.getInstance());
        shareDirectory.addFocusListener(AutoRomanListener.getInstance());
        
        stateMgr.controlSendMml();
        
    }
    
    
    class MmlModel {
        
        public void populate(ProjectStub stub) {
            
            boolean sending = stub.getSendMML();
            sendNoMML.setSelected(! sending);
            sendMML.setSelected(sending);
            //mml3.setEnabled(sending);
            //mml23.setEnabled(sending);
            //protocolCombo.setEnabled(sending);
            //uploaderServer.setEnabled(sending);
            //shareDirectory.setEnabled(sending);
            
            // V3 MML Version and Sending
            String val = stub.getMMLVersion();
            if (val != null && val.startsWith("2")) {
                mml23.setSelected(true);
            } else {
                mml3.setSelected(true);
            }
            
            // ���M��
            val = stub.getUploaderIPAddress();
            if (val != null && ! val.equals("")) {
                uploaderServer.setText(val);
            }
            
            // ���M�f�B���N�g��
            val = stub.getUploadShareDirectory();
            if (val != null && ! val.equals("")) {
                shareDirectory.setText(val);
            }
            
            connect();
        }
        
        public void restore(ProjectStub stub) {
            // �Z���^�[���M
            boolean b = sendMML.isSelected();
            stub.setSendMML(b);
            
            // MML �o�[�W����
            String val = mml3.isSelected() ? "300" : "230";
            stub.setMMLVersion(val);
            
            // �A�b�v���[�_�A�h���X
            val = uploaderServer.getText().trim();
            if (! val.equals("")) {
                stub.setUploaderIPAddress(val);
            }
            
            // ���L�f�B���N�g��
            val = shareDirectory.getText().trim();
            if (! val.equals("")) {
                stub.setUploadShareDirectory(val);
            }
        }
        
    }
    
    class StateMgr {
        
        public void checkState() {
            
            AbstractSettingPanel.State newState = isValid()
            ? AbstractSettingPanel.State.VALID_STATE
                    : AbstractSettingPanel.State.INVALID_STATE;
            if (newState != state) {
                setState(newState);
            }
        }
        
        public void controlSendMml() {
            boolean b = sendMML.isSelected();
            mml3.setEnabled(b);
            mml23.setEnabled(b);
            protocolCombo.setEnabled(b);
            uploaderServer.setEnabled(b);
            shareDirectory.setEnabled(b);
            this.checkState();
        }
        
        protected boolean isValid() {
            if (sendMML.isSelected()) {
                boolean uploadAddrOk = (uploaderServer.getText().trim().equals("") == false) ? true : false;
                boolean shareOk = (shareDirectory.getText().trim().equals("") == false) ? true : false;
                
                return (uploadAddrOk && shareOk) ? true : false;
            } else {
                return true;
            }
        }
    }
}