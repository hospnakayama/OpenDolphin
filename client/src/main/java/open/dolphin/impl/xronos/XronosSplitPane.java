/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package open.dolphin.impl.xronos;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import open.dolphin.client.Chart;
import open.dolphin.client.ClientContext;
import open.dolphin.helper.DBTask;
import open.dolphin.project.Project;
import open.dolphin.util.Log;
import open.dolphin.utilities.control.ImageIconEx;
import open.dolphin.utilities.control.TableEx;
import open.dolphin.utilities.utility.HttpConnect;
import open.dolphin.utilities.utility.XmlReadWrite;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Xronos連携
 * @author S.Oh@Life Sciences Computing Corporation.
 */
public class XronosSplitPane extends JSplitPane {
    //private static final String URL_HTTP = "http://";
    //private static final String URL_STUDYPATH = "/DicomServer/rest/studylist.aspx?";
    //private static final String URL_SERIESPATH = "/DicomServer/rest/serieslist.aspx?";
    //private static final String URL_IMAGEPATH = "/DicomServer/basis/search_Form.aspx?";
    private static final String FUNC_DOUBLECLICK = "doubleClicked";
    private static final String FUNC_SINGLECLICK = "singleClicked";
    private static final String FUNC_KEYUP = "keyUp";
    private static final String ATTRIBUTE_STUDYDATE = "StudyDate";
    private static final String ATTRIBUTE_MODALITY = "Mod";
    private static final String ATTRIBUTE_BODY = "Body";
    private static final String ATTRIBUTE_ACCNO = "AccNo";
    private static final String ATTRIBUTE_SRSCNT = "srCnt";
    private static final String ATTRIBUTE_IMGCNT = "imgCnt";
    private static final String ATTRIBUTE_STDINSTUID = "StudyInsUID";
    private static final String ATTRIBUTE_SRSNUMBER = "SrNumber";
    private static final String ATTRIBUTE_SRSDESCRIPT = "SrDesc";
    private static final String ATTRIBUTE_PROTOCOL = "Protocol";
    private static final String ATTRIBUTE_SRSIMGCNT = "cnt";
    private static final String ROOT_ERROR = "Error";
    private static final String ELEMENT_MESSAGE = "Message";
    private static final String TAG_USERID = "userid=";
    private static final String TAG_PATIENTID = "pid=";
    private static final String TAG_APPNAME = "app=";
    private static final String TAG_STDINSUID = "stuid=";
    private static final String TAG_THUMB = "thumb=";
    private static final int STUDYLIST_COLUMNWIDTH = 100;
    private static final int STUDYLIST_CELLHEIGHT = 30;
    private static final int SERIESLIST_COLUMNWIDTH = 200;
    private static final int SERIESLIST_CELLHEIGHT = 100;
    private static final int SCROLL_OFFSET = 90;
    private static final String THUMB_SIZE = "128";
    private static final Color STUDY_CELL1 = new Color(255,255,255);
    private static final Color STUDY_CELL2 = new Color(237,243,254);
    //private static final Color STUDY_CELL1 = new Color(105,105,48);
    //private static final Color STUDY_CELL2 = new Color(88,88,82);
    //private static final Color STUDY_BG = new Color(126,133,150);
    private static final Color STUDY_BG = new Color(255,255,255);
    private static final Color SERIES_CELL = new Color(120,136,177);
    private static final Color SERIES_BG = new Color(116,116,107);
    private static final int STUDYLIST_COLUMNNUM = 6;
    private static final int STUDYLIST_COLUMN_STUDYDATE = 0;
    private static final int STUDYLIST_COLUMN_MODALITY = 1;
    private static final int STUDYLIST_COLUMN_BODY = 2;
    private static final int STUDYLIST_COLUMN_ACCNO = 3;
    private static final int STUDYLIST_COLUMN_SRSCNT = 4;
    private static final int STUDYLIST_COLUMN_IMGCNT = 5;
    private static final int SERIESLIST_COLUMNNUM = 6;
    private static final int SERIESLIST_COLUMN_SRSNUMBER = 0;
    private static final int SERIESLIST_COLUMN_MODALITY = 1;
    private static final int SERIESLIST_COLUMN_BODY = 2;
    private static final int SERIESLIST_COLUMN_SRSDESCRIPT = 3;
    private static final int SERIESLIST_COLUMN_PROTOCOL = 4;
    private static final int SERIESLIST_COLUMN_SRSIMGCNT = 5;
    private static final String URL_DUMMY = "&dummy=dummy";
    private String[] studyListColumn = { "検査日付", "モダリティ", "検査部位", "受付番号", "シリーズ数", "イメージ数" };
    private Chart chart;
    private TableEx studyList;
    private TableEx seriesList;
    private HttpConnect http;
    private XmlReadWrite xmlStudy;
    private XmlReadWrite xmlSeries;
    private int beforeSelRow;
    private String studyURL;
    private String seriesURL;
    private String imageURL;
    private String userID;
    private String patientID;
    private String appName;
    private boolean processing;

    /**
     * コンストラクタ
     * @param host
     * @param user
     * @param patient
     * @param app 
     */
    public XronosSplitPane(Chart chart, String user, String patient, String app) {
        http = new HttpConnect();
        xmlStudy = new XmlReadWrite();
        xmlSeries = new XmlReadWrite();
        beforeSelRow = -1;
        //userID = null;
        //patientID = null;
        //appName = null;
        //studyURL = URL_HTTP + hostName + URL_STUDYPATH;
        //seriesURL = URL_HTTP + hostName + URL_SERIESPATH;
        //imageURL = URL_HTTP + hostName + URL_IMAGEPATH;
        studyURL = Project.getString(XronosLinkDocument.KEY_XRONOSBROWSER_STUDY, "");
        seriesURL = Project.getString(XronosLinkDocument.KEY_XRONOSBROWSER_SERIES, "");
        imageURL = Project.getString(XronosLinkDocument.KEY_XRONOSBROWSER_IMAGE, "");
        this.chart = chart;
        userID = user;
        patientID = patient;
        appName = app;
        processing = false;
        
        createStudyList();
        createSeriesList();
        
        this.setOrientation(JSplitPane.VERTICAL_SPLIT);
        this.setTopComponent(studyList.getPane());
        this.setBottomComponent(seriesList.getPane());
        setDividerSize(10);
        setContinuousLayout(false);
        setOneTouchExpandable(true);
        //setDividerLocation(dividerLocation);
    }
    
    /**
     * つまみ部分の自動計算
     */
    public void reDividerLocation(int height) {
        setDividerLocation(height - SERIESLIST_CELLHEIGHT - SCROLL_OFFSET);
    }
    
    /**
     * スタディリストの作成
     */
    private void createStudyList() {
        int[] widths = new int[studyListColumn.length];
        widths[0] = STUDYLIST_COLUMNWIDTH * 2;
        for(int i = 1; i < studyListColumn.length; i++) {
            widths[i] = STUDYLIST_COLUMNWIDTH;
        }
        studyList = new TableEx(studyListColumn, widths, false);
        studyList.setCellHeight(STUDYLIST_CELLHEIGHT);
        //studyList.setTableCellColor(CELL_COLOR1, CELL_COLOR2, Color.BLACK, null, null, null, null);
        //studyList.setTableCellColor(STUDY_CELL1, STUDY_CELL2, Color.WHITE, null, Color.BLACK, null, Color.BLACK);
        studyList.setTableCellColor(STUDY_CELL1, STUDY_CELL2, Color.BLACK, null, Color.WHITE, null, Color.WHITE);
        studyList.setBackColor(STUDY_BG);
        try {
            studyList.setLMouseUpMethod(this, FUNC_SINGLECLICK);
            studyList.setKeyUpMethod(this, FUNC_KEYUP);
        } catch (NoSuchMethodException ex) {
            //Logger.getLogger(XronosLinkPanel.class.getName()).log(Level.SEVERE, null, ex);
            Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
        }

        refresh(null);
    }
    
    /**
     * シリーズリストの作成
     */
    private void createSeriesList() {
        seriesList = new TableEx(null, null, false);
        seriesList.setCellHeight(SERIESLIST_CELLHEIGHT);
        seriesList.setHideColumn(true);
        seriesList.setTableCellMode(true);
        //seriesList.setTableCellColor(null, null, Color.BLACK, null, null, null, null);
        seriesList.setTableCellColor(SERIES_CELL, null, Color.WHITE, null, null, null, null);
        seriesList.setBackColor(SERIES_BG);
        try {
            seriesList.setLMouseDoubleClickMethod(this, FUNC_DOUBLECLICK);
        } catch (NoSuchMethodException ex) {
            //Logger.getLogger(XronosLinkPanel.class.getName()).log(Level.SEVERE, null, ex);
            Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
        }
    }
    
    /**
     * シリーズテキストの作成
     * @param text
     * @return 
     */
    private String createSeriesText(String[] text) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        if(text[0].length() > 0) sb.append("<nobr>No.").append(text[0]).append("</nobr><br>");
        if(text[1].length() > 0 && text[2].length() > 0) sb.append("<nobr>").append(text[1]).append(" / ").append(text[2]).append("</nobr><br>");
        else if(text[1].length() > 0) sb.append("<nobr>").append(text[1]).append("</nobr><br>");
        else if(text[2].length() > 0) sb.append("<nobr>").append(text[1]).append("</nobr><br>");
        if(text[3].length() > 0) sb.append("<nobr>").append(text[3]).append("</nobr><br>");
        if(text[4].length() > 0) sb.append("<nobr>").append(text[4]).append("</nobr><br>");
        if(text[5].length() > 0) sb.append("<nobr>").append(text[5]).append(" 画像</nobr><br>");
        sb.append("</html>");
        return sb.toString();
    }
    
    /**
     * シングルクリック
     * @param row
     * @param col 
     */
    public void singleClicked(int row, int col, MouseEvent e) {
        if(processing) {
            Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, "処理中のため再度処理を行わない");
            return;
        }
        if(beforeSelRow == row) {
            return;
        }
        processing = true;
        beforeSelRow = row;
        DBTask task = new DBTask<Void, Void>(chart) {
            @Override
            protected Void doInBackground() throws Exception {
                Element ele = xmlStudy.getEle(xmlStudy.getRoot(), beforeSelRow);
                String url = seriesURL + TAG_THUMB + THUMB_SIZE + "&" + TAG_USERID + userID + "&" + TAG_APPNAME + appName + "&" + TAG_STDINSUID + xmlStudy.getAtrbValue(ele, ATTRIBUTE_STDINSTUID);
                Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, url);
                try {
                    String get = http.httpGET(url, "GET");
                    try {
                        if(get.length() > 0 && xmlSeries.analize(get, http.getCharName()) == true) {
                            if(xmlSeries.getRoot().getTagName().equals(ROOT_ERROR) == true) {
                                String errMsg = xmlSeries.getEleVal(xmlSeries.getEle(xmlSeries.getRoot(), 0));
                                JOptionPane.showMessageDialog(null, errMsg, ROOT_ERROR, JOptionPane.ERROR_MESSAGE);
                            }else{
                                int num = xmlSeries.getEleNum(xmlSeries.getRoot());
                                int[] widths = new int[num];
                                for(int i = 0; i < num; i++) {
                                    widths[i] = SERIESLIST_COLUMNWIDTH;
                                }
                                seriesList.resetColumnCount(num, widths);
                                ImageIconEx[] img;
                                img = new ImageIconEx[num];
                                for(int i = 0; i < num; i++) {
                                    ele = xmlSeries.getEle(xmlSeries.getRoot(), i);
                                    String val = xmlSeries.getEleVal(ele);
                                    String[] atrb = new String[SERIESLIST_COLUMNNUM];
                                    atrb[SERIESLIST_COLUMN_SRSNUMBER] = xmlSeries.getAtrbValue(ele, ATTRIBUTE_SRSNUMBER);
                                    atrb[SERIESLIST_COLUMN_MODALITY] = xmlSeries.getAtrbValue(ele, ATTRIBUTE_MODALITY);
                                    atrb[SERIESLIST_COLUMN_BODY] = xmlSeries.getAtrbValue(ele, ATTRIBUTE_BODY);
                                    atrb[SERIESLIST_COLUMN_SRSDESCRIPT] = xmlSeries.getAtrbValue(ele, ATTRIBUTE_SRSDESCRIPT);
                                    atrb[SERIESLIST_COLUMN_PROTOCOL] = xmlSeries.getAtrbValue(ele, ATTRIBUTE_PROTOCOL);
                                    atrb[SERIESLIST_COLUMN_SRSIMGCNT] = xmlSeries.getAtrbValue(ele, ATTRIBUTE_SRSIMGCNT);
                                    img[i] = new ImageIconEx();
                                    img[i].setIcon(createBase64ToImage(val));
                                    img[i].setText(createSeriesText(atrb));
                                    for(int j = 0; j < SERIESLIST_COLUMNNUM; j++) {
                                        Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, atrb[j]);
                                    }
                                    Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, val);
                                }
                                seriesList.addData(img);
                            }
                        }
                    } catch (ParserConfigurationException ex) {
                        //Logger.getLogger(XronosLinkPanel.class.getName()).log(Level.SEVERE, null, ex);
                        Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
                    } catch (SAXException ex) {
                        //Logger.getLogger(XronosLinkPanel.class.getName()).log(Level.SEVERE, null, ex);
                        Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
                    }
                    http.httpGET(url + URL_DUMMY, "GET");
                } catch (MalformedURLException ex) {
                    //Logger.getLogger(XronosLinkPanel.class.getName()).log(Level.SEVERE, null, ex);
                    Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
                } catch (IOException ex) {
                    //Logger.getLogger(XronosLinkPanel.class.getName()).log(Level.SEVERE, null, ex);
                    Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
                }
                
                return null;
            }

            @Override
            protected void succeeded(Void result) {
                processing = false;
            }
            
            @Override
            protected void failed(Throwable e) {
                e.printStackTrace(System.err);
                StringBuilder why = new StringBuilder();
                why.append("画像情報の取得に失敗しました。");
                why.append("\n");
                Throwable cause = e.getCause();
                if (cause != null) {
                    why.append(cause.getMessage());
                } else {
                    why.append(e.getMessage());
                }
                Window parent = SwingUtilities.getWindowAncestor(context.getFrame());
                JOptionPane.showMessageDialog(parent, why.toString(), ClientContext.getFrameTitle(TITLE), JOptionPane.WARNING_MESSAGE);
                Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_WARNING, ClientContext.getFrameTitle(TITLE), why.toString(), (cause != null && cause.getMessage() != null) ? cause.getMessage() : e.getMessage());
                
                processing = false;
            }
        };
        task.execute();
    }
    
    /**
     * ダブルクリック
     * @param row
     * @param col 
     */
    public void doubleClicked(int row, int col, MouseEvent e) {
        int idx = studyList.getSelectedRow();
        Element ele = xmlStudy.getEle(xmlStudy.getRoot(), idx);
        String url = imageURL + TAG_USERID + userID + "&" + TAG_PATIENTID + patientID + "&" + TAG_STDINSUID + xmlStudy.getAtrbValue(ele, ATTRIBUTE_STDINSTUID);
        Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, url);
        // ブラウザの起動
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.browse(new URI(url));
        } catch (URISyntaxException ex) {
            //Logger.getLogger(XronosLinkPanel.class.getName()).log(Level.SEVERE, null, ex);
            Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
        } catch (IOException ex) {
            //Logger.getLogger(XronosLinkPanel.class.getName()).log(Level.SEVERE, null, ex);
            Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
        }
    }
    
    public void keyUp(int row, int col, KeyEvent e) {
        int code = e.getKeyCode();
        if(code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN || code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) {
            singleClicked(row, col, null);
        }
    }
    
    public void refresh(final String modality) {
        Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, (modality == null) ? "all" : modality);
        
        if(processing) {
            Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, "処理中のため再度処理を行わない");
            return;
        }
        processing = true;
        
        if(studyList != null) {
            studyList.clearAllData();
        }
        if(seriesList != null) {
            seriesList.clearAllData();
        }
        
        
        DBTask task = new DBTask<Void, Void>(chart) {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    String url = studyURL + TAG_USERID + userID + "&" + TAG_APPNAME + appName + "&" + TAG_PATIENTID + patientID;
                    Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, url);
                    String get = http.httpGET(url, "GET");
                    Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, get);
                    try {
                        if(get.length() > 0 && xmlStudy.analize(get, http.getCharName()) == true) {
                            if(xmlStudy.getRoot().getTagName().equals(ROOT_ERROR) == true) {
                                String errMsg = xmlStudy.getEleVal(xmlStudy.getEle(xmlStudy.getRoot(), 0));
                                JOptionPane.showMessageDialog(null, errMsg, ROOT_ERROR, JOptionPane.ERROR_MESSAGE);
                                Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, errMsg);
                            }else{
                                int num = xmlStudy.getEleNum(xmlStudy.getRoot());
                                for(int i = 0; i < num; i++) {
                                    Element ele = xmlStudy.getEle(xmlStudy.getRoot(), i);
                                    String val = xmlStudy.getEleVal(ele);
                                    String[] atrb = new String[STUDYLIST_COLUMNNUM];
                                    atrb[STUDYLIST_COLUMN_STUDYDATE] = xmlStudy.getAtrbValue(ele, ATTRIBUTE_STUDYDATE);
                                    atrb[STUDYLIST_COLUMN_MODALITY] = xmlStudy.getAtrbValue(ele, ATTRIBUTE_MODALITY);
                                    atrb[STUDYLIST_COLUMN_BODY] = xmlStudy.getAtrbValue(ele, ATTRIBUTE_BODY);
                                    atrb[STUDYLIST_COLUMN_ACCNO] = xmlStudy.getAtrbValue(ele, ATTRIBUTE_ACCNO);
                                    atrb[STUDYLIST_COLUMN_SRSCNT] = xmlStudy.getAtrbValue(ele, ATTRIBUTE_SRSCNT);
                                    atrb[STUDYLIST_COLUMN_IMGCNT] = xmlStudy.getAtrbValue(ele, ATTRIBUTE_IMGCNT);
                                    if(modality != null) {
                                        if(modality.equals(atrb[STUDYLIST_COLUMN_MODALITY])) {
                                            studyList.addData(atrb);
                                        }
                                    }else{
                                        studyList.addData(atrb);
                                    }
                                    for(int j = 0; j < STUDYLIST_COLUMNNUM; j++) {
                                        Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, atrb[j]);
                                    }
                                }
                            }
                        }
                    } catch (ParserConfigurationException ex) {
                        Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
                    } catch (SAXException ex) {
                        Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
                    }
                    http.httpGET(url + URL_DUMMY, "GET");
                } catch (MalformedURLException ex) {
                    Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
                } catch (IOException ex) {
                    Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
                }
                
                return null;
            }

            @Override
            protected void succeeded(Void result) {
                beforeSelRow = -1;
                processing = false;
            }
            
            @Override
            protected void failed(Throwable e) {
                e.printStackTrace(System.err);
                StringBuilder why = new StringBuilder();
                why.append("検査情報の取得に失敗しました。");
                why.append("\n");
                Throwable cause = e.getCause();
                if (cause != null) {
                    why.append(cause.getMessage());
                } else {
                    why.append(e.getMessage());
                }
                Window parent = SwingUtilities.getWindowAncestor(context.getFrame());
                JOptionPane.showMessageDialog(parent, why.toString(), ClientContext.getFrameTitle(TITLE), JOptionPane.WARNING_MESSAGE);
                Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_WARNING, ClientContext.getFrameTitle(TITLE), why.toString(), (cause != null && cause.getMessage() != null) ? cause.getMessage() : e.getMessage());
                
                beforeSelRow = -1;
                processing = false;
            }
        };

        task.execute();
    }
    
    /**
     * Base64のデコード
     * @param data
     * @return 
     */
    private ImageIcon createBase64ToImage(String data) {
        Base64Lib base64 = new Base64Lib();
        byte[] imgData = base64.decode(data);
        return new ImageIcon(imgData);
    }
}
/**
 * Base64のアクセスクラス
 * @author Life Sciences Computing Corporation.
 */
class Base64Lib {
    public Base64Lib() {
    }
    
    /**
     * エンコード
     * @param strInput
     * @return 
     */
    public String encode(String strInput) {
        return encode(strInput.getBytes());
    }
    
    /**
     * エンコード
     * @param input
     * @return 
     */
    public String encode(byte[] input) {
        return Base64.encodeBase64String(input);
    }
    
    /**
     * デコード
     * @param input
     * @return 
     */
    public byte[] decode(String input) {
        return Base64.decodeBase64(input);
    }
}
