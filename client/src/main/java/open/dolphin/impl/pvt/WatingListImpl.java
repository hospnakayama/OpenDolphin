package open.dolphin.impl.pvt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import open.dolphin.client.*;
import open.dolphin.delegater.PVTDelegater;
import open.dolphin.impl.img.DefaultBrowserEx;
import open.dolphin.impl.server.PVTReceptionLink;
import open.dolphin.impl.xronos.XronosLinkDocument;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.table.*;
import open.dolphin.util.AgeCalculator;
import open.dolphin.util.Log;
import org.apache.commons.lang.time.DurationFormatUtils;

/**
 * 受付リスト。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class WatingListImpl extends AbstractMainComponent implements PropertyChangeListener {
    
    // オープンアイコン
//minagawa^ Icon Server    
    //public static final ImageIcon OPEN_ICON = ClientContext.getImageIcon("open_16.gif");    
    //public static final ImageIcon NETWORK_ICON = ClientContext.getImageIcon("ntwrk_16.gif");
    public static final ImageIcon OPEN_ICON = ClientContext.getImageIconArias("icon_karte_open_state_small");    
    // ネットワークアイコン
    public static final ImageIcon NETWORK_ICON = ClientContext.getImageIconArias("icon_karte_open_someone_small");
//minagawa$    

    // Window Title
    private static final String NAME = "受付リスト";
    
    // 担当分のみを表示するかどうかの preference key
    private static final String ASSIGNED_ONLY = "assignedOnly";
    
//s.oh^ 2014/08/08 受付フィルタ(診療行為送信済)
    private static final String SENDED_ONLY = "sendedOnly";
//s.oh$
    
    // 修正送信アイコンの配列インデックス
    private static final int INDEX_MODIFY_SEND_ICON = 1;
    
    // 担当医未定の ORCA 医師ID
    private static final String UN_ASSIGNED_ID = "18080";
    
    // JTableレンダラ用の男性カラー
    //private static final Color MALE_COLOR = new Color(230, 243, 243);
    
    // JTableレンダラ用の女性カラー
    //private static final Color FEMALE_COLOR = new Color(254, 221, 242);
    
    // 受付キャンセルカラー
    private static final Color CANCEL_PVT_COLOR = new Color(128, 128, 128);
    
//    // その他カラー by pns
//    private static final Color SHOSHIN_COLOR = new Color(180,220,240); //青っぽい色
//    private static final Color KARTE_EMPTY_COLOR = new Color(250,200,160); //茶色っぽい色
//    private static final Color DIAGNOSIS_EMPTY_COLOR = new Color(243,255,15); //黄色
    
    // 来院テーブルのカラム名
    private static final String[] COLUMN_NAMES = {
        "受付", "患者ID", "来院時間", "氏   名", "性別", "保険", 
        "生年月日", "担当医", "診療科", "予約", "メモ", "状態"};
    
    // 来院テーブルのカラムメソッド
    private static final String[] PROPERTY_NAMES = {
        "getNumber", "getPatientId", "getPvtDateTrimDate", "getPatientName", "getPatientGenderDesc", "getFirstInsurance",
        "getPatientAgeBirthday", "getDoctorName", "getDeptName", "getAppointment", "getMemo", "getStateInteger"};
    
    // 来院テーブルのクラス名
    private static final Class[] COLUMN_CLASSES = {
        Integer.class, String.class, String.class, String.class, String.class, String.class, 
        String.class, String.class, String.class, String.class, String.class, Integer.class};
    
    // 来院テーブルのカラム幅
    private static final int[] COLUMN_WIDTH = {
        20, 80, 60, 100, 40, 130, 
        130, 50, 60, 40, 80, 30};
    
    // 年齢生年月日メソッド 
    private final String[] AGE_METHOD = {"getPatientAgeBirthday", "getPatientBirthday"};
    
    // カラム仕様名
    private static final String COLUMN_SPEC_NAME = "pvtTable.column.spec";
    
    // state Clumn Identifier
    private static final String COLUMN_IDENTIFIER_STATE = "stateColumn";
   
    // カラム仕様ヘルパー
    private ColumnSpecHelper columnHelper;

    // 受付時間カラム
    private int visitedTimeColumn;
    
    // 性別カラム
    private int sexColumn;
    
    // 年齢表示カラム
    private int ageColumn;
    
    // 来院情報テーブルのメモカラム
    private int memoColumn;
    
    // 来院情報テーブルのステータスカラム
    private int stateColumn;
    
    // 受付番号カラム
    private int numberColumn;

    // PVT Table 
    private JTable pvtTable;
    
    // Table Model
    private ListTableModel<PatientVisitModel> pvtTableModel;
    
    // TableSorter
    private ListTableSorter sorter;
    
    // 性別レンダラフラグ 
    private boolean sexRenderer;
    
//s.oh^ 2014/04/15 保険のレンダラ
    private boolean insuranceRenderer;
//s.oh$
    
    // 年齢表示 
    private boolean ageDisplay;
    
    // 選択されている行を保存
    private int selectedRow;
    
    // View class
    private WatingListView view;
    
    // 更新時刻フォーマッタ
    private SimpleDateFormat timeFormatter;
    
    // Chart State
    private Integer[] chartBitArray = {
        new Integer(PatientVisitModel.BIT_OPEN), 
        new Integer(PatientVisitModel.BIT_MODIFY_CLAIM),
        new Integer(PatientVisitModel.BIT_SAVE_CLAIM)};
    
    // Chart State を表示するアイコン
    private ImageIcon[] chartIconArray = {
        OPEN_ICON, 
//minagawa^ Icon Server        
        //ClientContext.getImageIcon("sinfo_16.gif"), 
        //ClientContext.getImageIcon("flag_16.gif")};//flag_16.gif=red
        ClientContext.getImageIconArias("icon_karte_modified_small"), 
        ClientContext.getImageIconArias("icon_sent_claim_small")};//flag_16.gif=red
//minagawa$    
    
    // State ComboBox
    private Integer[] userBitArray = {0, 3, 4, 5, 6};
    private ImageIcon[] userIconArray = {
        null, 
//minagawa^ Icon Server        
//        ClientContext.getImageIcon("apps_16.gif"), 
//        ClientContext.getImageIcon("fastf_16.gif"), 
//        ClientContext.getImageIcon("cart_16.gif"), 
//        ClientContext.getImageIcon("cancl_16.gif")};
        ClientContext.getImageIconArias("icon_under_treatment_small"), 
        ClientContext.getImageIconArias("icon_emergency_small"), 
        ClientContext.getImageIconArias("icon_under_shopping_small"), 
        ClientContext.getImageIconArias("icon_cancel_small")};
//minagawa$    
    private ImageIcon modifySendIcon;
    
    // Status　情報　メインウィンドウの左下に表示される内容
    private String statusInfo;

    // State 設定用のcombobox model
    private BitAndIconPair[] stateComboArray;
    
    // State 設定用のcombobox
    private JComboBox stateCmb;
    
    private AbstractAction copyAction;

    // 受付数・待ち時間の更新間隔
    //private static final int intervalSync = 60;
    private static final int intervalSync = 30;

    // pvtUpdateTask
    private ScheduledExecutorService executor;
    private ScheduledFuture schedule;
    private Runnable timerTask;
    
    // pvtCount
    private int totalPvtCount;
    private int waitingPvtCount;
    private Date waitingPvtDate;
    
    // PatientVisitModelの全部
    private List<PatientVisitModel> pvtList;
    
    // pvt delegater
    private PVTDelegater pvtDelegater;
    
    // Commet staff
    private String clientUUID;
    private ChartEventHandler cel;
    private String orcaId;
    
//s.oh^ 2014/08/19 受付バーコード対応
    private JDialog barcodeDialog;
//s.oh$

    /**
     * Creates new WatingList
     */
    public WatingListImpl() {
        setName(NAME);
        cel = ChartEventHandler.getInstance();
        clientUUID = cel.getClientUUID();
        orcaId = Project.getUserModel().getOrcaId();
//s.oh^ 2014/02/24 担当分のみ表示不具合
        if(orcaId == null) {
            Project.setBoolean(ASSIGNED_ONLY, false);
        }
//s.oh$
    }
    
    /**
     * プログラムを開始する。
     */
    @Override
    public void start() {
        setup();
        initComponents();
        connect();
        startSyncMode();
    }
    
    private void setup() {
        
        // ColumnSpecHelperを準備する
        columnHelper = new ColumnSpecHelper(COLUMN_SPEC_NAME,
                COLUMN_NAMES, PROPERTY_NAMES, COLUMN_CLASSES, COLUMN_WIDTH);
        columnHelper.loadProperty();

        // Scan して age, memo, state カラムを設定する
        visitedTimeColumn = columnHelper.getColumnPosition("getPvtDateTrimDate");
        sexColumn = columnHelper.getColumnPosition("getPatientGenderDesc");
        ageColumn = columnHelper.getColumnPositionEndsWith("Birthday");
        memoColumn = columnHelper.getColumnPosition("getMemo");
        stateColumn = columnHelper.getColumnPosition("getStateInteger");
        numberColumn = columnHelper.getColumnPosition("getNumber");
        
        // 修正送信アイコンを決める
        if (Project.getBoolean("change.icon.modify.send", true)) {
//minagawa^ Icon Server            
            //modifySendIcon = ClientContext.getImageIcon("sinfo_16.gif");
            modifySendIcon = ClientContext.getImageIconArias("icon_karte_modified_small");
//minagawa$            
        } else {
//minagawa^ Icon Server            
            //modifySendIcon = ClientContext.getImageIcon("flag_16.gif");
            modifySendIcon = ClientContext.getImageIconArias("icon_sent_claim_small");
//minagawa$            
        }
        chartIconArray[INDEX_MODIFY_SEND_ICON] = modifySendIcon;

        stateComboArray = new BitAndIconPair[userBitArray.length];
        for (int i = 0; i < userBitArray.length; i++) {
            stateComboArray[i] = new BitAndIconPair(userBitArray[i], userIconArray[i]);
        }
        stateCmb = new JComboBox(stateComboArray);
        ComboBoxRenderer renderer = new ComboBoxRenderer();
        renderer.setPreferredSize(new Dimension(30, ClientContext.getHigherRowHeight()));
        stateCmb.setRenderer(renderer);
        stateCmb.setMaximumRowCount(userBitArray.length);

        sexRenderer = Project.getBoolean("sexRenderer", false);
        ageDisplay = Project.getBoolean("ageDisplay", true);
        timeFormatter = new SimpleDateFormat("HH:mm");
//s.oh^ 2014/04/15 保険のレンダラ
        insuranceRenderer = Project.getBoolean("insuranceRenderer", false);
//s.oh$
        
        executor = Executors.newSingleThreadScheduledExecutor();
        
        pvtDelegater = PVTDelegater.getInstance();
        
        // 来院リスト
        pvtList = new ArrayList<PatientVisitModel>();
    }
    
    /**
     * GUI コンポーネントを初期化しレアイアウトする。
     */
    private void initComponents() {

        // View クラスを生成しこのプラグインの UI とする
        view = new WatingListView();
        setUI(view);

        view.getPvtInfoLbl().setText("");
        pvtTable = view.getTable();
        
        // ColumnSpecHelperにテーブルを設定する
        columnHelper.setTable(pvtTable);
        
        //------------------------------------------
        // View のテーブルモデルを置き換える
        //------------------------------------------
        String[] columnNames = columnHelper.getTableModelColumnNames();
        String[] methods = columnHelper.getTableModelColumnMethods();
        Class[] cls = columnHelper.getTableModelColumnClasses();

        pvtTableModel = new ListTableModel<PatientVisitModel>(columnNames, 0, methods, cls) {

            @Override
            public boolean isCellEditable(int row, int col) {

                boolean canEdit = true;

                // メモか状態カラムの場合
                canEdit = canEdit && ((col == memoColumn) || (col == stateColumn));

                // null でない場合
                canEdit = canEdit && (getObject(row) != null);

                if (!canEdit) {
                    return false;
                }

                // statusをチェックする
                PatientVisitModel pvt = getObject(row);

                if (pvt.getStateBit(PatientVisitModel.BIT_CANCEL)) {
                    // cancel case
                    canEdit = false;

                } else {
                    // Chartビットがたっている場合は不可
                    for (int i = 0; i < chartBitArray.length; i++) {
                        if (pvt.getStateBit(chartBitArray[i])) {
//s.oh^ 2014/10/14 診察終了後のメモ対応
                            //canEdit = false;
                            if(col != memoColumn) {
                                canEdit = false;
                            }
//s.oh$
                            break;
                        }
                    }
                }
                
//s.oh^ 不具合修正
                // insert funabashi（ステータスアイコンがずれてしまう対応）
                if(canEdit){
                    int index=0;
                    for (int i = 1; i < userBitArray.length; i++) {
                        if (pvt.getStateBit(userBitArray[i])) {
                            index = i;
                            break;
                        }
                    }
                    stateCmb.setSelectedIndex(index);
                }
//s.oh$

                return canEdit;
            }

            @Override
            public Object getValueAt(int row, int col) {

                Object ret = null;

                if (col == ageColumn && ageDisplay) {

                    PatientVisitModel p = getObject(row);

                    if (p != null) {
                        int showMonth = Project.getInt("ageToNeedMonth", 6);
                        ret = AgeCalculator.getAgeAndBirthday(p.getPatientModel().getBirthday(), showMonth);
                    }
                } else {

                    ret = super.getValueAt(row, col);
                }

                return ret;
            }

            @Override
            public void setValueAt(Object value, int row, int col) {

                // ここはsorterから取得したらダメ
                //final PatientVisitModel pvt = (PatientVisitModel) sorter.getObject(row);
                final PatientVisitModel pvt = pvtTableModel.getObject(row);
                
                if (pvt == null || value == null) {
                    return;
                }

                // Memo
                if (col == memoColumn) {
                    String memo = ((String) value).trim();
//s.oh^ 不具合修正
                    //if (memo != null && (!memo.equals(""))) {
                    //    pvt.setMemo(memo);
                    //    cel.publishPvtState(pvt);
                    //}
                    
                    // update start funabashi
                    if(pvt.getMemo() != null && pvt.getMemo().trim().equals(memo.trim())){
                        return; //データが変更していないので
                    }
                    if(pvt.getMemo() == null && memo.trim().length()==0 ){
                        return; // データが変更していないので
                    }
                    pvt.setMemo(memo);
//s.oh^ 2014/10/14 診察終了後のメモ対応
                    //cel.publishPvtState(pvt);
                    cel.publishPvtMemo(pvt);
//s.oh$
                    // update end funabashi
//s.oh$

                } else if (col == stateColumn) {

                    // State ComboBox の value
                    BitAndIconPair pair = (BitAndIconPair) value;
                    int theBit = pair.getBit().intValue();

                    if (theBit == PatientVisitModel.BIT_CANCEL) {
//s.oh^ 不具合修正
                        stateCmb.hidePopup();   // add funabashi リストが消えない対応
//s.oh$

                        Object[] cstOptions = new Object[]{"はい", "いいえ"};

                        StringBuilder sb = new StringBuilder(pvt.getPatientName());
                        sb.append("様の受付をキャンセルしますか?");
                        String msg = sb.toString();

                        int select = JOptionPane.showOptionDialog(
                                SwingUtilities.getWindowAncestor(pvtTable),
                                msg,
                                ClientContext.getFrameTitle(getName()),
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
//minagawa^ Icon Server                                
                                //ClientContext.getImageIcon("cancl_32.gif"),
                                ClientContext.getImageIconArias("icon_caution"),
//minagawa$                                
                                cstOptions, "はい");
                        Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_OTHER, ClientContext.getFrameTitle(getName()), msg);

                        System.err.println("select=" + select);

                        if (select != 0) {
                            Log.outputOperLogDlg(null, Log.LOG_LEVEL_0, "いいえ");
                            return;
                        }
                        Log.outputOperLogDlg(null, Log.LOG_LEVEL_0, "はい");
                    }

//s.oh^ 不具合修正
                    int oldState = pvt.getState();
//s.oh$
                    // unset all
                    pvt.setState(0);

                    // set the bit
                    if (theBit != 0) {
                        pvt.setStateBit(theBit, true);
                    }
                    
//s.oh^ 不具合修正
                    // add funabashi
                    if(pvt.getState() == oldState){
                        return; //データが変更していないので
                    }
//s.oh$

                    cel.publishPvtState(pvt);
                }
            }
        };

        // sorter組み込み
        sorter = new ListTableSorter(pvtTableModel);
        pvtTable.setModel(sorter);
        sorter.setTableHeader(pvtTable.getTableHeader());

        // 選択モード
        pvtTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Memo 欄 clickCountToStart=1
        JTextField tf = new JTextField();
        tf.addFocusListener(AutoKanjiListener.getInstance());
        DefaultCellEditor de = new DefaultCellEditor(tf);
        de.setClickCountToStart(1);
        pvtTable.getColumnModel().getColumn(memoColumn).setCellEditor(de);

        // 性別レンダラを生成する
        MaleFemaleRenderer sRenderer = new MaleFemaleRenderer();
        sRenderer.setTable(pvtTable);
        //sRenderer.setDefaultRenderer();
        
        // Center Renderer
        CenterRenderer centerRenderer = new CenterRenderer();
        centerRenderer.setTable(pvtTable);

        List<ColumnSpec> columnSpecs = columnHelper.getColumnSpecs();
        for (int i = 0; i < columnSpecs.size(); i++) {
            
            if (i == visitedTimeColumn || i == sexColumn) {
                pvtTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);

            } else if (i == stateColumn) {
                // カルテ(PVT)状態レンダラ
                KarteStateRenderer renderer = new KarteStateRenderer();
                renderer.setTable(pvtTable);
                renderer.setHorizontalAlignment(JLabel.CENTER);
                pvtTable.getColumnModel().getColumn(i).setCellRenderer(renderer);

            } else {
                pvtTable.getColumnModel().getColumn(i).setCellRenderer(sRenderer);
            }
        }

        // PVT状態設定エディタ
        pvtTable.getColumnModel().getColumn(stateColumn).setCellEditor(new DefaultCellEditor(stateCmb));
        pvtTable.getColumnModel().getColumn(stateColumn).setIdentifier(COLUMN_IDENTIFIER_STATE);

        // カラム幅更新
        columnHelper.updateColumnWidth();
        
        // 行高
        if (ClientContext.isWin()) {
            pvtTable.setRowHeight(ClientContext.getMoreHigherRowHeight());
        } else {
            pvtTable.setRowHeight(ClientContext.getHigherRowHeight());
        }
        
        if (pvtTable != null) {
            String method = ageDisplay ? AGE_METHOD[0] : AGE_METHOD[1];
            pvtTableModel.setProperty(method, ageColumn);
            for (int i = 0; i < columnSpecs.size(); i++) {
                ColumnSpec cs = columnSpecs.get(i);
                String test = cs.getMethod();
                if (test.toLowerCase().endsWith("birthday")) {
                    cs.setMethod(method);
                    break;
                }
            }
        }
    }

    /**
     * コンポーネントにイベントハンドラーを登録し相互に接続する。
     */
    private void connect() {

        // ColumnHelperでカラム変更関連イベントを設定する
        columnHelper.connect();
        
        // 来院リストテーブル 選択
        pvtTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    selectedRow = pvtTable.getSelectedRow();
                    controlMenu();
                }
            }
        });

        // 来院リストテーブル ダブルクリック
        view.getTable().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openKarte();
                }
            }
        });

        // コンテキストメニューを登録する
        view.getTable().addMouseListener(new ContextListener());

        // 靴のアイコンをクリックした時来院情報を検索する
        view.getKutuBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // 同期モードではPvtListを取得し直し
                getFullPvt();
            }
        });
        
//s.oh^ Xronos連携
        if(Project.getBoolean(XronosLinkDocument.KEY_XRONOSBROWSER_LINK) && view.getXronosBtn() != null) {
            view.getXronosBtn().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String imageURL = Project.getString(XronosLinkDocument.KEY_XRONOSBROWSER_IMAGE, "");
                    String url = imageURL + "userid=" + Project.getUserId();
                    Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, url);
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.browse(new URI(url));
                    } catch (URISyntaxException ex) {
                        Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
                    } catch (IOException ex) {
                        Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_ERROR, ex.getMessage());
                    }
                }
            });
        }
//s.oh$

        //-----------------------------------------------
        // Copy 機能を実装する
        //-----------------------------------------------
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        copyAction = new AbstractAction("コピー") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                copyRow();
            }
        };
        pvtTable.getInputMap().put(copy, "Copy");
        pvtTable.getActionMap().put("Copy", copyAction);
    }

    // comet long polling機能を設定する
    private void startSyncMode() {
        setStatusInfo();
        getFullPvt();
//minagawa^        
        //cel.addListener(this);
        cel.addPropertyChangeListener(this);
//minagawa$        
        timerTask = new UpdatePvtInfoTask();
        restartTimer();
        enter();
    }
    
    /**
     * タイマーをリスタートする。
     */
    private void restartTimer() {

        if (schedule != null && !schedule.isCancelled()) {
            if (!schedule.cancel(true)) {
                return;
            }
        }

        // 同期モードでは毎分０秒に待ち患者数を更新する
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(now.getTime());
        gc.clear(GregorianCalendar.SECOND);
        gc.clear(GregorianCalendar.MILLISECOND);
        gc.add(GregorianCalendar.MINUTE, 1);
        long delay = gc.getTimeInMillis() - now.getTimeInMillis();
        long interval = intervalSync * 1000;

        //schedule = executor.scheduleWithFixedDelay(timerTask, delay, interval, TimeUnit.MILLISECONDS);
        schedule = executor.scheduleWithFixedDelay(timerTask, 60, interval, TimeUnit.MILLISECONDS);
    }
    
    /**
     * メインウインドウのタブで受付リストに切り替わった時 コールされる。
     */
    @Override
    public void enter() {
        controlMenu();
//s.oh^ 不要機能の削除
        //getContext().getStatusLabel().setText(statusInfo);
        getContext().getStatusLabel().setText("");
//s.oh$
    }

    /**
     * プログラムを終了する。
     */
    @Override
    public void stop() {
        
        // ColumnSpecsを保存する
        if (columnHelper != null) {
            columnHelper.saveProperty();
        }
        // ChartStateListenerから除去する
        //cel.removeListener(this);
        cel.removePropertyChangeListener(this);
    }


    /**
     * 性別レンダラかどうかを返す。
     *
     * @return 性別レンダラの時 true
     */
    public boolean isSexRenderer() {
        return sexRenderer;
    }
    
//s.oh^ 2014/04/15 保険のレンダラ
    public boolean isInsuranceRenderer() {
        return insuranceRenderer;
    }
//s.oh$

    /**
     * レンダラをトグルで切り替える。
     */
    public void switchRenderere() {
        sexRenderer = !sexRenderer;
        Project.setBoolean("sexRenderer", sexRenderer);
        if(sexRenderer) {
            Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "性別レンダラを使用する");
        }else{
            Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "偶数奇数レンダラを使用する");
        }
        if (pvtTable != null) {
            pvtTableModel.fireTableDataChanged();
        }
    }
    
//s.oh^ 2014/04/15 保険のレンダラ
    public void switchInsuranceRenderere() {
        insuranceRenderer = !insuranceRenderer;
        Project.setBoolean("insuranceRenderer", insuranceRenderer);
        if(insuranceRenderer) {
            Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "保険(自費)を強調する");
        }
        if (pvtTable != null) {
            pvtTableModel.fireTableDataChanged();
        }
    }
//s.oh$

    /**
     * 年齢表示をオンオフする。
     */
    public void switchAgeDisplay() {
        if (pvtTable != null) {
            ageDisplay = !ageDisplay;
            Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "年齢表示", String.valueOf(ageDisplay));
            Project.setBoolean("ageDisplay", ageDisplay);
            String method = ageDisplay ? AGE_METHOD[0] : AGE_METHOD[1];
            pvtTableModel.setProperty(method, ageColumn);
            List<ColumnSpec> columnSpecs = columnHelper.getColumnSpecs();
            for (int i = 0; i < columnSpecs.size(); i++) {
                ColumnSpec cs = columnSpecs.get(i);
                String test = cs.getMethod();
                if (test.toLowerCase().endsWith("birthday")) {
                    cs.setMethod(method);
                    break;
                }
            }
        }
    }

    /**
     * テーブル及び靴アイコンの enable/diable 制御を行う。
     *
     * @param busy pvt 検索中は true
     */
    private void setBusy(final boolean busy) {
        
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (busy) {
                    view.getKutuBtn().setEnabled(false);
                    if (getContext().getCurrentComponent() == getUI()) {
                        getContext().block();
                        getContext().getProgressBar().setIndeterminate(true);
                    }
                    selectedRow = pvtTable.getSelectedRow();
                } else {
                    view.getKutuBtn().setEnabled(true);
                    if (getContext().getCurrentComponent() == getUI()) {
                        getContext().unblock();
                        getContext().getProgressBar().setIndeterminate(false);
                        getContext().getProgressBar().setValue(0);
                    }
                    pvtTable.getSelectionModel().addSelectionInterval(selectedRow, selectedRow);
                }
            }
        });
    }

    /**
     * 選択されている来院情報を設定返す。
     *
     * @return 選択されている来院情報
     */
    public PatientVisitModel getSelectedPvt() {
        selectedRow = pvtTable.getSelectedRow();
        return (PatientVisitModel) sorter.getObject(selectedRow);
    }

    /**
     * カルテオープンメニューを制御する。
     */
    private void controlMenu() {
        PatientVisitModel pvt = getSelectedPvt();
        boolean enabled = canOpen(pvt);
        getContext().enabledAction(GUIConst.ACTION_OPEN_KARTE, enabled);
    }

    public void openKarte() {

        PatientVisitModel pvt = getSelectedPvt();
        if (pvt == null) {
            return;
        }
        Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "カルテを開く", pvt.getPatientId());
        getContext().openKarte(pvt);
    }

    /**
     * カルテを開くことが可能かどうかを返す。
     *
     * @return 開くことが可能な時 true
     */
    private boolean canOpen(PatientVisitModel pvt) {
        
        if (pvt == null) {
            return false;
        }
        // Cancelなら開けない
        if (pvt.getStateBit(PatientVisitModel.BIT_CANCEL)) {
            return false;
        }
        // 開いてたら開けない
        if (pvt.getStateBit(PatientVisitModel.BIT_OPEN)) {
            return false;
        }
        return true;
    }

    /**
     * 受付リストのコンテキストメニュークラス。
     */
    private class ContextListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            mabeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mabeShowPopup(e);
        }

        public void mabeShowPopup(MouseEvent e) {

            if (e.isPopupTrigger()) {

                final JPopupMenu contextMenu = new JPopupMenu();
                String pop3 = "偶数奇数レンダラを使用する";
                String pop4 = "性別レンダラを使用する";
                String pop5 = "年齢表示";
                String pop6 = "担当分のみ表示";
                String pop7 = "修正送信を注意アイコンにする";

                int row = pvtTable.rowAtPoint(e.getPoint());
                PatientVisitModel obj = getSelectedPvt();
                
                if (row == selectedRow && obj != null && !obj.getStateBit(PatientVisitModel.BIT_CANCEL)) {
                    String pop1 = "カルテを開く";
                    contextMenu.add(new JMenuItem(
                            new ReflectAction(pop1, WatingListImpl.this, "openKarte")));
                    contextMenu.addSeparator();
                    contextMenu.add(new JMenuItem(copyAction));
                    
                    // pvt削除は誰も開いていない場合のみ
                    if (obj.getPatientModel().getOwnerUUID()==null) {
                        contextMenu.add(new JMenuItem(
                                new ReflectAction("受付削除", WatingListImpl.this, "removePvt")));
                    }
                    contextMenu.addSeparator();
                }
                
                // pvt cancelのundo
                if (row == selectedRow && obj != null && obj.getStateBit(PatientVisitModel.BIT_CANCEL)) {
                    contextMenu.add(new JMenuItem(
                            new ReflectAction("キャンセル取消", WatingListImpl.this, "undoCancelPvt")));
                    contextMenu.addSeparator();
                }
                
                JRadioButtonMenuItem oddEven = new JRadioButtonMenuItem(
                        new ReflectAction(pop3, WatingListImpl.this, "switchRenderere"));
                JRadioButtonMenuItem sex = new JRadioButtonMenuItem(
                        new ReflectAction(pop4, WatingListImpl.this, "switchRenderere"));
                ButtonGroup bg = new ButtonGroup();
                bg.add(oddEven);
                bg.add(sex);
                contextMenu.add(oddEven);
                contextMenu.add(sex);
                if (sexRenderer) {
                    sex.setSelected(true);
                } else {
                    oddEven.setSelected(true);
                }
//s.oh^ 2014/04/15 保険のレンダラ
                String pop8 = "保険(自費)を強調する";
                JRadioButtonMenuItem insurance = new JRadioButtonMenuItem(new ReflectAction(pop8, WatingListImpl.this, "switchInsuranceRenderere"));
                contextMenu.add(insurance);
                if(insuranceRenderer) {
                    insurance.setSelected(true);
                }
//s.oh$

                JCheckBoxMenuItem item = new JCheckBoxMenuItem(pop5);
                contextMenu.add(item);
                item.setSelected(ageDisplay);
                item.addActionListener(
                        EventHandler.create(ActionListener.class, WatingListImpl.this, "switchAgeDisplay"));

                // 担当分のみ表示: getOrcaId() != nullでメニュー
                if (orcaId != null) {
                    contextMenu.addSeparator();

                    // 担当分のみ表示
                    JCheckBoxMenuItem item2 = new JCheckBoxMenuItem(pop6);
                    contextMenu.add(item2);
                    item2.setSelected(isAssignedOnly());
                    item2.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            boolean now = isAssignedOnly();
                            Project.setBoolean(ASSIGNED_ONLY, !now);
                            Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "担当分のみ表示", String.valueOf(!now));
                            filterPatients();
                        }
                    });
                }
//s.oh^ 2014/02/24 担当分のみ表示不具合
                else{
                    Project.setBoolean(ASSIGNED_ONLY, false);
                    Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "担当分のみ表示 ORCA ID = null");
                    //filterPatients();
                }
//s.oh$
                
//s.oh^ 2014/08/08 受付フィルタ(診療行為送信済)
                contextMenu.addSeparator();
                // 診療行為未送信分のみ表示
                JCheckBoxMenuItem item2 = new JCheckBoxMenuItem("診療行為送信分を非表示");
                contextMenu.add(item2);
                item2.setSelected(isSendedOnly());
                item2.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        boolean now = isSendedOnly();
                        Project.setBoolean(SENDED_ONLY, !now);
                        Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "診療行為未送信分のみ表示", String.valueOf(!now));
                        filterPatients();
                    }
                });
//s.oh$

                // 修正送信を注意アイコンにする ON/OF default = ON
                JCheckBoxMenuItem item3 = new JCheckBoxMenuItem(pop7);
                contextMenu.add(item3);
                item3.setSelected(Project.getBoolean("change.icon.modify.send", true));
                item3.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "修正送信を注意アイコンにする");
                        boolean curIcon = Project.getBoolean("change.icon.modify.send", true);
                        boolean change = !curIcon;
                        Project.setBoolean("change.icon.modify.send", change);
                        changeModiSendIcon();
                    }
                });
                
                JMenu menu = columnHelper.createMenuItem();
                contextMenu.add(menu);

                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    /**
     * 修正送信アイコンを決める
     *
     * @param change
     */
    private void changeModiSendIcon() {

        // 修正送信アイコンを決める
        if (Project.getBoolean("change.icon.modify.send", true)) {
//minagawa^ Icon Server            
            //modifySendIcon = ClientContext.getImageIcon("sinfo_16.gif");
            modifySendIcon = ClientContext.getImageIconArias("icon_karte_modified_small");
//minagawa$            
        } else {
//minagawa^ Icon Server             
            //modifySendIcon = ClientContext.getImageIcon("flag_16.gif");
            modifySendIcon = ClientContext.getImageIconArias("icon_sent_claim_small");
//minagawa$            
        }
        chartIconArray[INDEX_MODIFY_SEND_ICON] = modifySendIcon;

        // 表示を更新する
        pvtTableModel.fireTableDataChanged();
    }

    /**
     * 選択されている行をコピーする。
     */
    public void copyRow() {
        Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "コピー");

        StringBuilder sb = new StringBuilder();
        int numRows = pvtTable.getSelectedRowCount();
        int[] rowsSelected = pvtTable.getSelectedRows();
        int numColumns = pvtTable.getColumnCount();

        for (int i = 0; i < numRows; i++) {
            if (sorter.getObject(rowsSelected[i]) != null) {
                StringBuilder s = new StringBuilder();
                for (int col = 0; col < numColumns; col++) {
                    Object o = pvtTable.getValueAt(rowsSelected[i], col);
                    if (o != null) {
                        s.append(o.toString());
                    }
                    s.append(",");
                }
                if (s.length() > 0) {
                    s.setLength(s.length() - 1);
                }
                sb.append(s.toString()).append("\n");
            }
        }
        if (sb.length() > 0) {
            StringSelection stsel = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
            Log.outputFuncLog(Log.LOG_LEVEL_0, sb.toString());
        }
    }

    /**
     * 選択した患者の受付キャンセルをundoする。masuda
     */
    public void undoCancelPvt() {

        final PatientVisitModel pvtModel = getSelectedPvt();
        Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "キャンセル取消", pvtModel.getPatientId());

        // ダイアログを表示し確認する
        StringBuilder sb = new StringBuilder(pvtModel.getPatientName());
        sb.append("様の受付キャンセルを取り消しますか?");
        if (!showCancelDialog(sb.toString())) {
            return;
        }
        
        // updateStateする。
        pvtModel.setStateBit(PatientVisitModel.BIT_CANCEL, false);
        cel.publishPvtState(pvtModel);
    }
    
    /**
     * 選択した患者の受付を削除する。masuda
     */
    public void removePvt() {

        final PatientVisitModel pvtModel = getSelectedPvt();

        // ダイアログを表示し確認する
        StringBuilder sb = new StringBuilder(pvtModel.getPatientName());
        sb.append("様の受付を削除しますか?");
        if (!showCancelDialog(sb.toString())) {
            return;
        }

        // publish
        cel.publishPvtDelete(pvtModel);
/*
        SwingWorker worker = new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() throws Exception {

                // サーバーから削除するとあとでStateMsgModelが届く
                pvtDelegater.removePvt(pvtModel.getId());
                return null;
            }
        };
        worker.execute();
*/
    }
    
    private boolean showCancelDialog(String msg) {

        final String[] cstOptions = new String[]{"はい", "いいえ"};

        int select = JOptionPane.showOptionDialog(
                SwingUtilities.getWindowAncestor(pvtTable),
                msg,
                ClientContext.getFrameTitle(getName()),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
//minagawa^ Icon Server                
                //ClientContext.getImageIcon("cancl_32.gif"),
                ClientContext.getImageIconArias("icon_caution"),
//minagawa$                
                cstOptions, cstOptions[1]);
        Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_OTHER, ClientContext.getFrameTitle(getName()), msg);
        if(select == 0) {
            Log.outputOperLogDlg(null, Log.LOG_LEVEL_0, "はい");
        }else{
            Log.outputOperLogDlg(null, Log.LOG_LEVEL_0, "いいえ");
        }
        return (select == 0);
    }

    private class BitAndIconPair {

        private Integer bit;
        private ImageIcon icon;

        public BitAndIconPair(Integer bit, ImageIcon icon) {
            this.bit = bit;
            this.icon = icon;
        }

        public Integer getBit() {
            return bit;
        }

        public ImageIcon getIcon() {
            return icon;
        }
    }
    
    // 左下のstatus infoを設定する
    private void setStatusInfo() {

        StringBuilder sb = new StringBuilder();
        sb.append("更新間隔: ");
        sb.append(intervalSync);
        sb.append("秒 ");
        sb.append("同期");
        statusInfo = sb.toString();
    }

    // 更新時間・待ち人数などを設定する
    private void updatePvtInfo() {

        String waitingTime = "00:00";
        Date now = new Date();

        final StringBuilder sb = new StringBuilder();
        sb.append(timeFormatter.format(now));
        sb.append(" | ");
        sb.append("来院数");
        sb.append(String.valueOf(totalPvtCount));
        sb.append(" 待ち");
        sb.append(String.valueOf(waitingPvtCount));
        sb.append(" 待時間 ");
        if (waitingPvtDate != null && now.after(waitingPvtDate)){
            waitingTime = DurationFormatUtils.formatPeriod(waitingPvtDate.getTime(), now.getTime(), "HH:mm");
        }
        sb.append(waitingTime);
        view.getPvtInfoLbl().setText(sb.toString());
    }

//pns^
    /**
     * 来院数，待人数，待時間表示, modified by masuda
     */
    private void countPvt() {

        waitingPvtCount = 0;
        totalPvtCount = 0;
        waitingPvtDate = null;

        List<PatientVisitModel> dataList = pvtTableModel.getDataProvider();

        for (int i = 0; i < dataList.size(); i++) {
            PatientVisitModel pvt = dataList.get(i);
            if (!pvt.getStateBit(PatientVisitModel.BIT_SAVE_CLAIM) && 
                    !pvt.getStateBit(PatientVisitModel.BIT_MODIFY_CLAIM) &&
                    !pvt.getStateBit(PatientVisitModel.BIT_CANCEL)) {
                // 診察未終了レコードをカウント，最初に見つかった未終了レコードの時間から待ち時間を計算
                ++waitingPvtCount;
                if (waitingPvtDate == null) {
                    waitingPvtDate = ModelUtils.getDateTimeAsObject(pvt.getPvtDate());
                }
            }
            if (!pvt.getStateBit(PatientVisitModel.BIT_CANCEL)) {
                ++totalPvtCount;
            }
        }
    }
//pns$
    // 最終行を表示する
    private void showLastRow() {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                int lastRow = pvtTableModel.getObjectCount() - 1;
                pvtTable.scrollRectToVisible(pvtTable.getCellRect(lastRow, 0, true));
            }
        });
    }
    
    private class UpdatePvtInfoTask implements Runnable {
//minagawa^ 言いがかり対応          
        private boolean fullPvt;
        
        public UpdatePvtInfoTask() {
//s.oh^ 2013/07/31 赤旗問題対応
            //fullPvt = Project.getBoolean("pvt.timer.fullpvt", false);
            fullPvt = Project.getBoolean(Project.PVT_TIMER_CHECK, false);
//s.oh$
        }

        @Override
        public void run() {
            if (fullPvt) {
                //view.getKutuBtn().doClick();
//s.oh^ 2013/11/06 受付定期チェック処理変更
                //getFullPvt();
                updateFullPvt();
//s.oh$
            } else {
                // 同期時は時刻と患者数を更新するのみ
                updatePvtInfo();
            }
//minagawa$            
        }
    }
    
    // pvtを全取得する
    private void getFullPvt() {

        SwingWorker worker = new SwingWorker<List<PatientVisitModel>, Void>() {

            @Override
            protected List<PatientVisitModel> doInBackground() throws Exception {
                setBusy(true);
                // サーバーからpvtListを取得する
                return pvtDelegater.getPvtList();
            }

            @Override
            protected void done() {
                try {
                    List<PatientVisitModel> ret = get();
                    if (ret!=null && ret.size()>0) {
                        pvtList = ret;
                        for(PatientVisitModel pvm : pvtList) {
                            Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, "患者：", pvm.getPatientId());
                        }
                    }
                    // フィルタリング
                    filterPatients();
                    // 最終行までスクロール
                    showLastRow();
                    countPvt();
                    updatePvtInfo();
                } catch (InterruptedException | ExecutionException ex) {
                }
                setBusy(false);
            }
        };
        worker.execute();
    }
    
//s.oh^ 2013/11/06 受付定期チェック処理変更
    private void updateFullPvt() {
        if(pvtDelegater == null) return;

        view.getKutuBtn().setEnabled(false);
        if (getContext().getCurrentComponent() == getUI()) {
            getContext().block();
            getContext().getProgressBar().setIndeterminate(true);
        }
        final int row = pvtTable.getSelectedRow();
        
        SwingWorker worker = new SwingWorker<List<PatientVisitModel>, Void>() {
            @Override
            protected List<PatientVisitModel> doInBackground() throws Exception {
                return pvtDelegater.getPvtList();
            }

            @Override
            protected void done() {
                try {
                    pvtList = get();
                    filterPatients();
                    countPvt();
                    updatePvtInfo();
                } catch (InterruptedException | ExecutionException ex) {
                }
                
                view.getKutuBtn().setEnabled(true);
                if (getContext().getCurrentComponent() == getUI()) {
                    getContext().unblock();
                    getContext().getProgressBar().setIndeterminate(false);
                    getContext().getProgressBar().setValue(0);
                }
                if(row >= 0) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            pvtTable.getSelectionModel().addSelectionInterval(row, row);
                            //Rectangle r = pvtTable.getCellRect(row, row, true);
                            //pvtTable.scrollRectToVisible(r);
                        }
                    });
                }else if(pvtTable.getRowCount() > 0) {
                    //showLastRow();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            pvtTable.getSelectionModel().addSelectionInterval(pvtTable.getRowCount()-1, pvtTable.getRowCount()-1);
                        }
                    });
                }
            }
        };
        worker.execute();
    }
//s.oh$
    
    // 受付番号を振り、フィルタリングしてtableModelに設定する
    private void filterPatients() {

        List<PatientVisitModel> list = new ArrayList<PatientVisitModel>();
        List<PatientVisitModel> listTmp = new ArrayList<PatientVisitModel>();
        
//s.oh^ ORCAIDがない場合は全部表示 2013/08/08
        //if (isAssignedOnly() && pvtList!=null) {
        if (isAssignedOnly() && pvtList!=null && orcaId != null) {
//s.oh$
            for (PatientVisitModel pvt : pvtList) {
                String doctorId = pvt.getDoctorId();
                if (doctorId == null || doctorId.equals(orcaId) || doctorId.equals(UN_ASSIGNED_ID)) {
                    listTmp.add(pvt);
                }
            }
        } else if (pvtList!=null) {
            listTmp.addAll(pvtList);
        }
        
//s.oh^ 2014/08/08 受付フィルタ(診療行為送信済)
        if(isSendedOnly()) {
            for(PatientVisitModel pvt : listTmp) {
                if((pvt.getState() & (1 << PatientVisitModel.BIT_SAVE_CLAIM)) > 0 || (pvt.getState() & (1 << PatientVisitModel.BIT_MODIFY_CLAIM)) > 0) {
                }else{
                    list.add(pvt);
                }
            }
        }else{
            list.addAll(listTmp);
        }
//s.oh$
        
        for (int i = 0; i < list.size(); ++i) {
            PatientVisitModel pvt = list.get(i);
            pvt.setNumber(i + 1);
        }
        pvtTableModel.setDataProvider(list);
        //pvtTable.repaint();
    }
    
    private boolean isAssignedOnly() {
        return Project.getBoolean(ASSIGNED_ONLY, false);
    }
    
//s.oh^ 2014/08/08 受付フィルタ(診療行為送信済)
    private boolean isSendedOnly() {
        return Project.getBoolean(SENDED_ONLY, false);
    }
//s.oh$
    
//s.oh^ 2014/08/19 受付バーコード対応
    public void receiptBarcode() {
        final JTextField infoField = new JTextField(25);
        infoField.addFocusListener(AutoRomanListener.getInstance());
        infoField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openReceipt(infoField.getText());
                }
            }
        });
        JPanel inputPanel = new JPanel();
        inputPanel.setBackground(Color.white);
        inputPanel.setOpaque(true);
        inputPanel.add(new JLabel("受付情報"));
        inputPanel.add(infoField);
        JButton open = new JButton("開く");
        open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                    openReceipt(infoField.getText());
            }
        });
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.white);
        btnPanel.setOpaque(true);
        btnPanel.add(open);
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(Color.white);
        contentPane.setOpaque(true);
        contentPane.add(inputPanel, BorderLayout.CENTER);
        contentPane.add(btnPanel, BorderLayout.SOUTH);
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        barcodeDialog = new JDialog(new JFrame(), ClientContext.getString("productString"), true);
        barcodeDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        barcodeDialog.setContentPane(contentPane);
        barcodeDialog.getRootPane().setDefaultButton(open);
        barcodeDialog.pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int n = ClientContext.isMac() ? 3 : 2;
        int x = (screen.width - barcodeDialog.getPreferredSize().width) / 2;
        int y = (screen.height - barcodeDialog.getPreferredSize().height) / n;
        barcodeDialog.setLocation(x, y);
        barcodeDialog.setVisible(true);
    }
    
    public void openReceipt(String receipt) {
        Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_INFORMATION, receipt);
        barcodeDialog.setVisible(false);
        barcodeDialog.dispose();
        if(receipt == null) return;
        String[] info = receipt.split(",");
        if(info.length == 1) {
            // 患者ID
            for(int i = 0; i < pvtTableModel.getObjectCount(); i++) {
                PatientVisitModel pvt = pvtTableModel.getObject(i);
                if(pvt.getPatientId().equals(receipt)) {
                    Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "カルテを開く", pvt.getPatientId());
                    getContext().openKarte(pvt);
                    return;
                }
            }
        }else if(info.length == 2) {
            // 患者ID + 保険
            for(int i = 0; i < pvtTableModel.getObjectCount(); i++) {
                PatientVisitModel pvt = pvtTableModel.getObject(i);
                if(pvt.getFirstInsurance() == null) continue;
                if(pvt.getPatientId().equals(info[0]) && pvt.getFirstInsurance().equals(info[1])) {
                    Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "カルテを開く", pvt.getPatientId(), pvt.getDeptName());
                    getContext().openKarte(pvt);
                    return;
                }
            }
        }else if(info.length == 3) {
            // 患者ID + 保険 + 診療科
            for(int i = 0; i < pvtTableModel.getObjectCount(); i++) {
                PatientVisitModel pvt = pvtTableModel.getObject(i);
                if(pvt.getDeptName() == null || pvt.getFirstInsurance() == null) continue;
                if(pvt.getPatientId().equals(info[0]) && pvt.getFirstInsurance().indexOf(info[1]) >= 0 && pvt.getDeptName().equals(info[2])) {
                    Log.outputOperLogOper(null, Log.LOG_LEVEL_0, "カルテを開く", pvt.getPatientId(), pvt.getFirstInsurance(), pvt.getDeptName());
                    getContext().openKarte(pvt);
                    return;
                }
            }
        }else{
            
        }
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(pvtTable), "該当受付情報がありません（" + receipt + "）", "バーコード", JOptionPane.WARNING_MESSAGE);
        Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_WARNING, "該当受付情報がありません（" + receipt + "）");
    }
//s.oh$

 //minagawa^ propertyhangeに変更   
    @Override
    public void propertyChange(PropertyChangeEvent pce) {

        if (!pce.getPropertyName().equals(ChartEventHandler.CHART_EVENT_PROP)) {
            Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, "not CHART_EVENT_PROP", pce.getPropertyName());
            return;
        }
        
        ChartEventModel evt = (ChartEventModel)pce.getNewValue();
        int eventType = evt.getEventType();
        List<PatientVisitModel> tableDataList = pvtTableModel.getDataProvider();

        switch (eventType) {
            case ChartEventModel.PVT_ADD:
                PatientVisitModel model = evt.getPatientVisitModel();
                if (model==null) {
                    Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_INFORMATION, "PVT_ADD", "model = null");
                    break;
                }
                Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, "PVT_ADD", model.getPatientId());
                pvtList.add(model);
//s.oh^ 2013/11/26 スクロールバーのリセット
                int sRow = pvtTable.getSelectedRow();
//s.oh$
//minagawa^                
//                // 担当でないならばテーブルに追加しない
//                if (isAssignedOnly()) {
//                    String doctorId = model.getDoctorId();
//                    if (doctorId != null && !doctorId.equals(orcaId) && !doctorId.equals(UN_ASSIGNED_ID)) {
//                        break;
//                    }
//                }
//                int sRow = selectedRow;
//                pvtTableModel.addObject(model);
//                // 番号を振る
//                model.setNumber(tableDataList.size());
//minagawa$                
                filterPatients();
                // 選択中の行を保存
                // 保存した選択中の行を選択状態にする
//s.oh^ 2013/11/26 スクロールバーのリセット
                //int sRow = selectedRow;
                //pvtTable.getSelectionModel().addSelectionInterval(sRow, sRow);
                //// 追加した行は見えるようにスクロールする
                //showLastRow();
                if(Project.getBoolean("receipt.pvtadd.scrollbar.reset", true)) {
                    sRow = selectedRow;
                    pvtTable.getSelectionModel().addSelectionInterval(sRow, sRow);
                    showLastRow();
                }else{
                    selectedRow = sRow;
                    pvtTable.getSelectionModel().addSelectionInterval(sRow, sRow);
                }
//s.oh$
                
//s.oh^ 受付連携
                // ORCAクラウド接続
                String receptKind = Project.getString(Project.CLAIM_SENDER);
                if(receptKind != null && !receptKind.equals("client")) {
                    PVTReceptionLink link = new PVTReceptionLink();
                    if(Project.getBoolean("reception.csvlink", false)) {
                        link.receptionCSVLink(model);
                    }
                    if(Project.getBoolean("reception.csvlink2", false)) {
                        link.receptionCSVLink2(model);
                    }
                    if(Project.getBoolean("reception.csvlink3", false)) {
                        link.receptionCSVLink3(model);
                    }
                    if(Project.getBoolean("reception.xmllink", false)) {
                        link.receptionXMLLink(model);
                    }
                    if(Project.getBoolean("reception.link", false)) {
                        link.receptionLink(model);
                    }
                    if(Project.getBoolean("receipt.link", false)) {
                        link.receiptLink(model);
                    }
                }
//s.oh$
                
                break;
                
            case ChartEventModel.PVT_STATE:
                // pvtListを更新
                for (PatientVisitModel pvt : pvtList) {
                    if (pvt.getId() == evt.getPvtPk()) {
                        // 更新する
                        pvt.setState(evt.getState());
                        pvt.setByomeiCount(evt.getByomeiCount());
                        pvt.setByomeiCountToday(evt.getByomeiCountToday());
                        Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, "PVT_STATE", pvt.getPatientId(), String.valueOf(pvt.getState()), String.valueOf(pvt.getId()));
                        pvt.setMemo(evt.getMemo());
                    }
                    if (pvt.getPatientModel().getId() == evt.getPtPk()) {
                        String ownerUUID = evt.getOwnerUUID();
                        pvt.setStateBit(PatientVisitModel.BIT_OPEN, ownerUUID != null);
                        pvt.getPatientModel().setOwnerUUID(evt.getOwnerUUID());
                        Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, "PVT_STATE", pvt.getPatientId(), String.valueOf(pvt.getState()), String.valueOf(pvt.getPatientModel().getId()), evt.getOwnerUUID());
                    }
                }
                for (int row = 0; row < tableDataList.size(); ++row) {
                    PatientVisitModel pvt = tableDataList.get(row);
                    if (pvt.getId() == evt.getPvtPk() 
                            || pvt.getPatientModel().getId() == evt.getPtPk()) {
                        pvtTableModel.fireTableRowsUpdated(row, row);
                    }
                }
                break;
            case ChartEventModel.PVT_DELETE:
                // pvtListから削除
                PatientVisitModel toRemove = null;
                for (PatientVisitModel pvt : pvtList) {
                    if (evt.getPvtPk() == pvt.getId()) {
                        toRemove = pvt;
                        break;
                    }
                }
                if (toRemove != null) {
                    pvtList.remove(toRemove);
                }
                
                // 該当するpvtを削除し受付番号を振りなおす
                int counter = 0;
                toRemove = null;
                for (PatientVisitModel pm : tableDataList) {
                    if (pm.getId() == evt.getPvtPk()) {
                        toRemove = pm;
                    } else {
                        pm.setNumber(++counter);
                    }
                }
                if (toRemove != null) {
                    pvtTableModel.delete(toRemove);
                }
                Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_INFORMATION, "PVT_DELETE", toRemove.getPatientId());
                break;
                
            case ChartEventModel.PVT_RENEW:
                // 日付が変わるとCMD_RENEWが送信される。pvtListをサーバーから取得する
                getFullPvt();
                Log.outputFuncLog(Log.LOG_LEVEL_0, Log.FUNCTIONLOG_KIND_INFORMATION, "日付変更によるPvtListの取得");
                break;
                
            case ChartEventModel.PVT_MERGE:
                // 同じ時刻のPVTで、PVTには追加されず、患者情報や保険情報の更新のみの場合
                // pvtListに変更
                PatientVisitModel toMerge = evt.getPatientVisitModel();
                for (int i = 0; i < pvtList.size(); ++i) {
                    PatientVisitModel pvt = pvtList.get(i);
                    if (pvt.getId() == evt.getPvtPk()) {
                        // 受付番号を継承
                        int num = pvt.getNumber();
                        toMerge.setNumber(num);
                        pvtList.set(i, toMerge);
                        Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, "PVT_MERGE", toMerge.getPatientId());
                    }
                }
                // tableModelに変更
                for (int row = 0; row < tableDataList.size(); ++row) {
                    PatientVisitModel pvt = tableDataList.get(row);
                    if (pvt.getId() == evt.getPvtPk()) {
                        // 選択中の行を保存
                        sRow = selectedRow;
                        pvtTableModel.setObject(row, toMerge);
                        // 保存した選択中の行を選択状態にする
                        pvtTable.getSelectionModel().addSelectionInterval(sRow, sRow);
                        break;
                    }
                }
                break;
                
            case ChartEventModel.PM_MERGE:
                // 患者モデルに変更があった場合
                // pvtListに変更
                PatientModel pm = evt.getPatientModel();
                long pk = pm.getId();
                for (PatientVisitModel pvt : pvtList) {
                    if (pvt.getPatientModel().getId() == pk) {
                        pvt.setPatientModel(pm);
                        Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, "PM_MERGE", pvt.getPatientId());
                    }
                }
                break;
//s.oh^ 2014/10/14 診察終了後のメモ対応
            case ChartEventModel.PVT_MEMO:
                for(PatientVisitModel pvt : pvtList) {
                    if(pvt.getId() == evt.getPvtPk()) {
                        Log.outputFuncLog(Log.LOG_LEVEL_3, Log.FUNCTIONLOG_KIND_INFORMATION, "PVT_MEMO", pvt.getPatientId(), evt.getMemo(), String.valueOf(pvt.getId()));
                        pvt.setMemo(evt.getMemo());
                    }
                }
                for(int row = 0; row < tableDataList.size(); ++row) {
                    PatientVisitModel pvt = tableDataList.get(row);
                    if (pvt.getId() == evt.getPvtPk() 
                            || pvt.getPatientModel().getId() == evt.getPtPk()) {
                        pvtTableModel.fireTableRowsUpdated(row, row);
                    }
                }
                break;
//s.oh$
        }
        
        // PvtInfoを更新する
        countPvt();
        updatePvtInfo();
    }
    
       
//    /**
//     * KarteStateRenderer
//     * カルテ（チャート）の状態をレンダリングするクラス。
//     */
//    protected class KarteStateRenderer extends DefaultTableCellRenderer {
//        
//        /** Creates new IconRenderer */
//        public KarteStateRenderer() {
//            super();
//            setOpaque(true);
//        }
//        
//        @Override
//        public Component getTableCellRendererComponent(JTable table,
//                Object value,
//                boolean isSelected,
//                boolean isFocused,
//                int row, int col) {
//            
//            super.getTableCellRendererComponent(table, value, isSelected, isFocused, row, col);
//            PatientVisitModel pvt = (PatientVisitModel)sorter.getObject(row);
//            
//            if (isSelected) {
//                this.setBackground(table.getSelectionBackground());
//                this.setForeground(table.getSelectionForeground());
//                
//            } else {
//                
//                if (isSexRenderer()) {
//
//                    if (pvt !=null && pvt.getPatientModel().getGender().equals(IInfoModel.MALE)) {
//                        this.setBackground(GUIConst.TABLE_MALE_COLOR);
//                    } else if (pvt !=null && pvt.getPatientModel().getGender().equals(IInfoModel.FEMALE)) {
//                        this.setBackground(GUIConst.TABLE_FEMALE_COLOR);
//                    } else {
//                        this.setBackground(Color.WHITE);
//                    }
//
//                } else {
//                    if ((row & (1)) == 0) {
//                        this.setBackground(GUIConst.TABLE_EVEN_COLOR);
//                    } else {
//                        this.setBackground(GUIConst.TABLE_ODD_COLOR);
//                    }
//                }
//
//                //Color fore = pvt != null && (pvt.getState() & (1<<bitCancel))!=0 ? CANCEL_PVT_COLOR : table.getForeground();
//                Color fore = (pvt != null && pvt.getStateBit(PatientVisitModel.BIT_CANCEL)) ? CANCEL_PVT_COLOR : table.getForeground();
//                this.setForeground(fore);
//            }
//            
//            if (value!=null && col==stateColumn) {
//                
//                ImageIcon icon = null;
//                
//                // 最初に chart bit をテストする
//                for (int i = 0; i < chartBitArray.length; i++) {
//                    if (pvt.getStateBit(chartBitArray[i])) {
//                        if (i == PatientVisitModel.BIT_OPEN && 
//                                !clientUUID.equals(pvt.getPatientModel().getOwnerUUID())) {
//                            icon = NETWORK_ICON;
//                        } else {
//                            icon = chartIconArray[i];
//                        }
//                        break;
//                    }
//                }
//
//                // user bit をテストする
//                if (icon == null) {
//
//                    // bit 0 はパス
//                    for (int i = 1; i < userBitArray.length; i++) {
//                        if (pvt.getStateBit(userBitArray[i])) {
//                            icon = userIconArray[i];
//                            break;
//                        }
//                    }
//                }
//                
//                this.setIcon(icon);
//                this.setText("");
//                
//            } else {
//                setIcon(null);
//                this.setText(value == null ? "" : value.toString());
//            }
//            return this;
//        }
//    }
//    
//    /**
//     * KarteStateRenderer
//     * カルテ（チャート）の状態をレンダリングするクラス。
//     */
//    protected class MaleFemaleRenderer extends DefaultTableCellRenderer {
//        
//        /** Creates new IconRenderer */
//        public MaleFemaleRenderer() {
//            super();
//        }
//        
//        @Override
//        public Component getTableCellRendererComponent(JTable table,
//                Object value,
//                boolean isSelected,
//                boolean isFocused,
//                int row, int col) {
//            
//            super.getTableCellRendererComponent(table, value, isSelected, isFocused, row, col);
//            PatientVisitModel pvt = (PatientVisitModel) pvtTableModel.getObject(row);
//            
//            if (isSelected) {
//                this.setBackground(table.getSelectionBackground());
//                this.setForeground(table.getSelectionForeground());
//                
//            } else {
//                if (isSexRenderer()) {
//
//                    if (pvt !=null && pvt.getPatientModel().getGender().equals(IInfoModel.MALE)) {
//                        this.setBackground(GUIConst.TABLE_MALE_COLOR);
//                    } else if (pvt !=null && pvt.getPatientModel().getGender().equals(IInfoModel.FEMALE)) {
//                        this.setBackground(GUIConst.TABLE_FEMALE_COLOR);
//                    } else {
//                        this.setBackground(Color.WHITE);
//                    }
//
//                } else {
//
//                    if ((row & (1)) == 0) {
//                        this.setBackground(GUIConst.TABLE_EVEN_COLOR);
//                    } else {
//                        this.setBackground(GUIConst.TABLE_ODD_COLOR);//pvt.getStateBit(PatientVisitModel.BIT_CANCEL)
//                    }
//                }
//                
//                //Color fore = pvt != null && (pvt.getState() & (1<<bitCancel))!=0 ? CANCEL_PVT_COLOR : table.getForeground();
//                Color fore = (pvt != null && pvt.getStateBit(PatientVisitModel.BIT_CANCEL)) ? CANCEL_PVT_COLOR : table.getForeground();
//                this.setForeground(fore);
//            }
//            
//            if (value != null && value instanceof String) {
//                this.setText((String) value);
//            } else {
//                setIcon(null);
//                this.setText(value == null ? "" : value.toString());
//            }
//            return this;
//        }
//    }
//    
//    protected class CenterRenderer extends MaleFemaleRenderer {
//        
//        /** Creates new IconRenderer */
//        public CenterRenderer() {
//            super();
//            this.setHorizontalAlignment(JLabel.CENTER);
//        }
//    }
//
//    /**
//     * Iconを表示するJComboBox Renderer.
//     */
//    protected class ComboBoxRenderer extends JLabel
//                           implements ListCellRenderer {
//
//        public ComboBoxRenderer() {
//            setOpaque(true);
//            setHorizontalAlignment(CENTER);
//            setVerticalAlignment(CENTER);
//        }
//
//        @Override
//        public Component getListCellRendererComponent(
//                                           JList list,
//                                           Object value,
//                                           int index,
//                                           boolean isSelected,
//                                           boolean cellHasFocus) {
//
//            BitAndIconPair pair = (BitAndIconPair)value;
//
//            if (isSelected) {
//                setBackground(list.getSelectionBackground());
//                setForeground(list.getSelectionForeground());
//            } else {
//                setBackground(list.getBackground());
//                setForeground(list.getForeground());
//            }
//
//            setIcon(pair.getIcon());
//            return this;
//        }
//    }
    
    /**
     * KarteStateRenderer カルテ（チャート）の状態をレンダリングするクラス。
     */
    private class KarteStateRenderer extends StripeTableCellRenderer {

        /** Creates new IconRenderer */
        public KarteStateRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean isFocused,
                int row, int col) {

            super.getTableCellRendererComponent(table, value, isSelected, isFocused, row, col);
            
            PatientVisitModel pvt = (PatientVisitModel)sorter.getObject(row);
            Color fore = (pvt != null && pvt.getStateBit(PatientVisitModel.BIT_CANCEL))
                    ? CANCEL_PVT_COLOR 
                    : table.getForeground();
            this.setForeground(fore);
            
            // 選択状態の場合はStripeTableCellRendererの配色を上書きしない
            if(pvt != null && pvt.getStateBit(PatientVisitModel.BIT_CANCEL)) {
                this.setForeground(CANCEL_PVT_COLOR);
            }else if (pvt != null && !isSelected) {
                if (isSexRenderer()) {
                    if (IInfoModel.MALE.equals(pvt.getPatientModel().getGender())) {
                        this.setBackground(GUIConst.TABLE_MALE_COLOR);
                    } else if (IInfoModel.FEMALE.equals(pvt.getPatientModel().getGender())) {
                        this.setBackground(GUIConst.TABLE_FEMALE_COLOR);
                    }
                }
//                // 病名の状態に応じて背景色を変更 pns
//                if (!pvt.getStateBit(PatientVisitModel.BIT_CANCEL)) {
//                    // 初診
//                    if (pvt.isShoshin()) {
//                        this.setBackground(SHOSHIN_COLOR);
//                    }
//                    // 病名ついてない
//                    if (!pvt.hasByomei()) {
//                        this.setBackground(DIAGNOSIS_EMPTY_COLOR);
//                    }
//                }
//s.oh^ 2014/04/15 保険のレンダラ
                if (isInsuranceRenderer()) {
                    if (pvt.getFirstInsurance() != null && pvt.getFirstInsurance().startsWith(IInfoModel.INSURANCE_SELF_PREFIX)) {
                        this.setBackground(Color.YELLOW);
                    }
                }
//s.oh$
            }
            
            boolean bStateColumn = (pvtTable.getColumnModel().getColumn(col).getIdentifier()!=null &&
                                    pvtTable.getColumnModel().getColumn(col).getIdentifier().equals(COLUMN_IDENTIFIER_STATE));

            if (value != null && bStateColumn) {

                ImageIcon icon = null;

                // 最初に chart bit をテストする
                for (int i = 0; i < chartBitArray.length; i++) {
                    if (pvt.getStateBit(chartBitArray[i])) {
                        if (i == PatientVisitModel.BIT_OPEN && 
                                !clientUUID.equals(pvt.getPatientModel().getOwnerUUID())) {
                            icon = NETWORK_ICON;
                        } else {
                            icon = chartIconArray[i];
                        }
                        break;
                    }
                }

                // user bit をテストする
                if (icon == null) {

                    // bit 0 はパス
                    for (int i = 1; i < userBitArray.length; i++) {
                        if (pvt.getStateBit(userBitArray[i])) {
                            icon = userIconArray[i];
                            break;
                        }
                    }
                }

//                if (pvt.getStateBit(PatientVisitModel.BIT_UNFINISHED)) {
//                    setBackground(KARTE_EMPTY_COLOR);
//                }

                this.setIcon(icon);
                this.setText("");

            } else {
                setIcon(null);
                this.setText(value == null ? "" : value.toString());
            }
            return this;
        }
    }

    /**
     * KarteStateRenderer カルテ（チャート）の状態をレンダリングするクラス。
     */
    private class MaleFemaleRenderer extends StripeTableCellRenderer {

        public MaleFemaleRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean isFocused,
                int row, int col) {

            super.getTableCellRendererComponent(table, value, isSelected, isFocused, row, col);
            
            PatientVisitModel pvt = (PatientVisitModel)sorter.getObject(row);
            
            if (pvt != null) {
                if (pvt.getStateBit(PatientVisitModel.BIT_CANCEL)) {
                    this.setForeground(CANCEL_PVT_COLOR);
                } else {
                    // 選択状態の場合はStripeTableCellRendererの配色を上書きしない
                    if (isSexRenderer() && !isSelected) {
                        if (IInfoModel.MALE.equals(pvt.getPatientModel().getGender())) {
                            this.setBackground(GUIConst.TABLE_MALE_COLOR);
                        } else if (IInfoModel.FEMALE.equals(pvt.getPatientModel().getGender())) {
                            this.setBackground(GUIConst.TABLE_FEMALE_COLOR);
                        }
                    }
//s.oh^ 2014/04/15 保険のレンダラ
                    if (isInsuranceRenderer() && !isSelected) {
                        if (pvt.getFirstInsurance() != null && pvt.getFirstInsurance().startsWith(IInfoModel.INSURANCE_SELF_PREFIX)) {
                            this.setBackground(Color.YELLOW);
                        }
                    }
//s.oh$
                }
            }

            return this;
        }
    }

    private class CenterRenderer extends MaleFemaleRenderer {

        public CenterRenderer() {
            super();
            this.setHorizontalAlignment(JLabel.CENTER);
        }
    }

    /**
     * Iconを表示するJComboBox Renderer.
     */
    private class ComboBoxRenderer extends JLabel implements ListCellRenderer {

        public ComboBoxRenderer() {
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
        }

        @Override
        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            BitAndIconPair pair = (BitAndIconPair) value;

            setIcon(pair.getIcon());
            return this;
        }
    }
}