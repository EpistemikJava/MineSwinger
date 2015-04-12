/* ******************************************************************************
 * 
 *  Mark Sattolo (epistemik@gmail.com)
 * ----------------------------------------------- 
 *  $File: //depot/Eclipse/Java/workspace/MineSwinger/src/mhs/mineswinger/MineSwinger.java $
 *  $Revision: #13 $
 *  $Change: 155 $
 *  $DateTime: 2011/12/25 00:33:16 $
 * -----------------------------------------------
 * 
 * MineSwinger.java
 * Created on Jan 29, 2008, 18h18
 * Initial git version created Apr 12, 2015
 * 
 ********************************************************************************/

package mhs.mineswinger;

import java.awt.Color;
import java.awt.Event;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * This is a Java Swing MineSweeper game inspired by the classic Windows game.
 * 
 * @author  MARK SATTOLO (based on code by Mats Antell)
 * @version $Revision: #13 $
 * @see #MineSwinger(int)
 * @see #startup()
 * @see #createMineField()
 */
public class MineSwinger extends JFrame
{
 /*
  *     FIELDS
  * ================================================================================================== */
  
  /** Keep the compiler from complaining...  */
  private static final long serialVersionUID = 5744270357479325185L;
  
  final String VERSION = myname() + " $Revision: #13 $" ; //$NON-NLS-1$
  
  static final int TIMER_DELAY_MSEC = 1000 ;
  
  static final double DEFAULT_DENSITY = 0.21 ;
  
  static final String strDEFAULT_TYPEFACE = "Arial" ; //$NON-NLS-1$
  
  static final Color
                    COLOR_GAME_BKGRND = Color.cyan.darker()   , 
                   COLOR_SCORE_BKGRND = Color.yellow.darker() ,
                    COLOR_INFO_BKGRND = Color.yellow.darker() ,
                    COLOR_INFO_FRGRND = Color.blue.darker()   ;
  
  static final int
                  FIELD_SIZE_XS = 12 , // # of squares per side of the MineField
                  FIELD_SIZE_SM = 16 ,
                  FIELD_SIZE_MD = 20 ,
                  FIELD_SIZE_LG = 24 ,
                  FIELD_SIZE_XL = 30 ,
                  
                SQUARE_SIZE_SM = 18 , // length in pixels of each side of an individual square 
                SQUARE_SIZE_MD = 24 , 
                SQUARE_SIZE_LG = 32 , 
                
                  FONT_SIZE_SM = SQUARE_SIZE_SM/2 + 2 ,
                  FONT_SIZE_MD = SQUARE_SIZE_MD/2 + 4 ,
                  FONT_SIZE_LG = SQUARE_SIZE_LG/2 + 6 ,
                  
                      X_BORDER = 80 , // extra space around the mine field
                      Y_BORDER = 60 , // for the the labels, buttons, etc
                      
                    BOOM       =  0 , 
                    MOUSE      =  BOOM      + 1 ,
                    FLAG       =  MOUSE     + 1 ,
                    NEAT       =  FLAG      + 1 , 
                    EASY       =  NEAT      + 1 ,
                    AWESOME    =  EASY      + 1 , 
                    RESET      =  AWESOME   + 1 , 
                    BAD_CLEAR  =  RESET     + 1 , 
                    SLOW       =  BAD_CLEAR + 1 ,
                NUM_SND_CLIPS  =  SLOW      + 1 ; // must be last entry
  
  // these match up with the ints in the previous list
  static final String[] SND_CLIPS
                          = {
                              "sounds/boom.au"       , //$NON-NLS-1$
                              "sounds/mouse.au"      , //$NON-NLS-1$
                              "sounds/flag.au"       , //$NON-NLS-1$
                              "sounds/neat.au"       , //$NON-NLS-1$
                              "sounds/easy.au"       , //$NON-NLS-1$
                              "sounds/awesome.au"    , //$NON-NLS-1$
                              "sounds/reset.au"      , //$NON-NLS-1$
                              "sounds/bad_clear.au"  , //$NON-NLS-1$
                              "sounds/slow.au"         //$NON-NLS-1$
                            };
  
  protected int DEBUG_LEVEL ;
  
  static Font fontSMALL, fontMEDIUM, fontLARGE ;
  
  private boolean    isRunning = false,
                  settingsOpen = false,
                      qMarksOn = false ;
  
  private int          seconds, // accumulated game time
               scoreMultiplier, // increase scoring according to density, etc
               maxScore, currentScore = 0 ;
  
  /**
   *  length, in pixels, of each side of the <code>MineField</code><br>
   *    = (current side length of each square) * (# of squares on each side of field)
   */
  private int fieldDim ;
  
  private String   strBaseURL ;
  private boolean  soundsLoaded = false, soundsActive = false ;
  private java.applet.AudioClip[]  soundFX ;
  
  MineField     mineField ;
  MineSettings  settingsFrame ;

  private JLayeredPane  pane ; 
  private MineListener  listener ;
  private Timer         gameClock ;
  
  private KeyStroke  ks ;
  private JMenuBar   gameBar ;
  private JMenu      gameMenu, settingsMenu ;
  private JMenuItem  tinyFieldItem, smallFieldItem, medFieldItem, largeFieldItem, hugeFieldItem, 
                     exitGameItem, newGameItem, showSettingsItem ;
  
  private JCheckBoxMenuItem  qMarkItem ;
  
  private JPanel   scorePanel, infoPanel ;
  private JButton  soundBtn, resetBtn ;
  private Box      infoBox  ;
  
  private JLabel  infoMesg, timeMesg, scoreMesg, minesMesg ;
  private JLabel  timeTitle, scoreTitle, minesTitle ;
  
 /*
  *     METHODS
  * ================================================================================================== */
  
  /**
   * MAIN 
   * @param args - from command line
   * @see MineSwinger#MineSwinger(int)
   * @see MineSwinger#startup()
   */ 
  public static void main( final String[] args )
  {
    System.out.println( "PROGRAM STARTED ON " + Thread.currentThread() ); //$NON-NLS-1$
    
    SwingUtilities.invokeLater
    (
      new Runnable()
      {
        @Override
        public void run()
        {
          new MineSwinger( args.length > 0 ? Integer.parseInt(args[0]) : 0 ).startup();
        }
      }
    );
  }
  ////  END main()  //////////////////////////////////////////////////////////////////////////
  
  /**
   * CONSTRUCTOR
   * @param debug level
   * @see #buildFonts()
   * @see #buildComponents()
   */
  public MineSwinger( final int debug )
  {
    DEBUG_LEVEL = debug ;
    System.out.println( myname() + Msgs.str("debug_level") + DEBUG_LEVEL ); //$NON-NLS-1$
    
    setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    
    setTitle( VERSION );
    getContentPane().setBackground( COLOR_GAME_BKGRND );
    
    listener = new MineListener();
    gameClock = new Timer( TIMER_DELAY_MSEC, listener );
    
    buildFonts();
    buildComponents();
    
    setFocusable( true );
    setResizable( false );
    
  }// Constructor
  
  /**
   * build and set the JMenuBar; build the Panels and set in the ContentPane
   * @see #buildJMenuBar()
   * @see #buildScorePanel()
   * @see #buildInfoPanel()
   */
  private void buildComponents()
  {
    buildJMenuBar();
    setJMenuBar( gameBar );
    
    buildScorePanel();
    buildInfoPanel();
    
    getContentPane().add( scorePanel, "North" ); //$NON-NLS-1$
    getContentPane().add(  infoPanel, "South" ); //$NON-NLS-1$
    
  }// buildComponents()
  
  /**
   * build GameMenu and SettingsMenu and set in the JMenuBar
   * @see #buildGameMenu()
   * @see #buildSettingsMenu()
   */
  private void buildJMenuBar()
  {
    // create Menu Bar
    gameBar = new JMenuBar();
    
    buildGameMenu();
    buildSettingsMenu();
    
    gameBar.add( gameMenu  );
    gameBar.add( settingsMenu );
    
  }// buildJMenuBar()
  
  /** contains items for choosing Square size and for exiting the game  */
  private void buildGameMenu()
  {
    gameMenu = new JMenu( Msgs.str("Game") ); //$NON-NLS-1$
    
    tinyFieldItem = new JMenuItem( Msgs.str("field.xs") + FIELD_SIZE_XS + Msgs.str("sqr_side") ); //$NON-NLS-1$ //$NON-NLS-2$
    ks = KeyStroke.getKeyStroke( KeyEvent.VK_T, Event.ALT_MASK ); 
    tinyFieldItem.setAccelerator( ks );
    tinyFieldItem.setMnemonic( 'T' );
    tinyFieldItem.addActionListener( listener );
    
    smallFieldItem = new JMenuItem( Msgs.str("field.sm") + FIELD_SIZE_SM + Msgs.str("sqr_side") ); //$NON-NLS-1$ //$NON-NLS-2$
    ks = KeyStroke.getKeyStroke( KeyEvent.VK_S, Event.ALT_MASK ); 
    smallFieldItem.setAccelerator( ks );
    smallFieldItem.setMnemonic( 'S' );
    smallFieldItem.addActionListener( listener );
    
    medFieldItem = new JMenuItem( Msgs.str("field.md") + FIELD_SIZE_MD + Msgs.str("sqr_side") ); //$NON-NLS-1$ //$NON-NLS-2$
    ks = KeyStroke.getKeyStroke( KeyEvent.VK_M, Event.ALT_MASK ); 
    medFieldItem.setAccelerator( ks );
    medFieldItem.setMnemonic( 'M' );
    medFieldItem.addActionListener( listener );
    
    largeFieldItem = new JMenuItem( Msgs.str("field.lg") + FIELD_SIZE_LG + Msgs.str("sqr_side") ); //$NON-NLS-1$ //$NON-NLS-2$
    ks = KeyStroke.getKeyStroke( KeyEvent.VK_L, Event.ALT_MASK ); 
    largeFieldItem.setAccelerator( ks );
    largeFieldItem.setMnemonic( 'L' );
    largeFieldItem.addActionListener( listener );
    
    hugeFieldItem = new JMenuItem( Msgs.str("field.xl") + FIELD_SIZE_XL + Msgs.str("sqr_side") ); //$NON-NLS-1$ //$NON-NLS-2$
    ks = KeyStroke.getKeyStroke( KeyEvent.VK_H, Event.ALT_MASK ); 
    hugeFieldItem.setAccelerator( ks );
    hugeFieldItem.setMnemonic( 'H' );
    hugeFieldItem.addActionListener( listener );
    
    exitGameItem = new JMenuItem( Msgs.str("Exit") ); //$NON-NLS-1$
    ks = KeyStroke.getKeyStroke( KeyEvent.VK_X, Event.ALT_MASK ); 
    exitGameItem.setAccelerator( ks );
    exitGameItem.setMnemonic( 'X' );
    exitGameItem.addActionListener( listener );
    
    gameMenu.add( tinyFieldItem );
    gameMenu.add( smallFieldItem );
    gameMenu.add( medFieldItem );
    gameMenu.add( largeFieldItem );
    gameMenu.add( hugeFieldItem );
    gameMenu.addSeparator();
    gameMenu.add( exitGameItem );
    
  }// buildGameMenu()

  /** contains items to activate question marks, start a new game, and opening the MineSettings frame  */
  private void buildSettingsMenu()
  {
    settingsMenu = new JMenu( Msgs.str("Settings") ); //$NON-NLS-1$
    
    qMarkItem = new JCheckBoxMenuItem( Msgs.str("Qmarks"), qMarksOn ); //$NON-NLS-1$
    ks = KeyStroke.getKeyStroke( KeyEvent.VK_Q, Event.ALT_MASK ); 
    qMarkItem.setAccelerator( ks ); 
    qMarkItem.setMnemonic( 'Q' ); 
    qMarkItem.addActionListener( listener );
    
    newGameItem = new JMenuItem( Msgs.str("game.new") ); //$NON-NLS-1$
    ks = KeyStroke.getKeyStroke( KeyEvent.VK_F2, 0 ); 
    newGameItem.setAccelerator( ks ); 
    newGameItem.addActionListener( listener );
    
    showSettingsItem = new JMenuItem( Msgs.str("settings.change") ); //$NON-NLS-1$
    ks = KeyStroke.getKeyStroke( KeyEvent.VK_C, Event.ALT_MASK ); 
    showSettingsItem.setAccelerator( ks ); 
    showSettingsItem.setMnemonic( 'C' ); 
    showSettingsItem.addActionListener( listener );
    
    settingsMenu.add( qMarkItem );
    settingsMenu.add( newGameItem );
    settingsMenu.add( showSettingsItem );
    
  }// buildSettingsMenu()
  
  /** displays the current score, number of mines left (or found), and elapsed time  */
  private void buildScorePanel()
  {
    scorePanel = new JPanel( new GridLayout(1,6) );
    scorePanel.setBackground( COLOR_SCORE_BKGRND );
    
    scoreTitle = new JLabel( Msgs.str("Score") ); //$NON-NLS-1$
    scoreTitle.setHorizontalAlignment( SwingConstants.CENTER );
    scoreMesg = new JLabel( "0" ); //$NON-NLS-1$
    scorePanel.add( scoreTitle );   
    scorePanel.add( scoreMesg );
    
    minesTitle = new JLabel( Msgs.str("mines.left") ); //$NON-NLS-1$
    minesMesg = new JLabel( "0" ); //$NON-NLS-1$
    scorePanel.add( minesTitle );
    scorePanel.add( minesMesg );
    
    timeTitle = new JLabel( Msgs.str("Time") ); //$NON-NLS-1$
    timeMesg = new JLabel( Msgs.str("zeroTime") ); //$NON-NLS-1$
    scorePanel.add( timeTitle );
    scorePanel.add( timeMesg );
    
  }// buildScorePanel()
  
  /** contains the 'Activate/De-activate Sounds' button, the 'Reset' button;<br>
   *  -- also displays useful game info to the user, e.g. 'Ready'  */
  private void buildInfoPanel()
  {
    infoPanel = new JPanel( new GridLayout(1,1) );
    infoBox = Box.createHorizontalBox();
    
    // setBackground() does NOT work with a Box
    infoPanel.setBackground( COLOR_INFO_BKGRND );
    
    soundBtn = new JButton( Msgs.str("Snd.go") ); //$NON-NLS-1$
    soundBtn.addActionListener( listener );
    infoBox.add( Box.createGlue() );
    infoBox.add( soundBtn );
    
    resetBtn = new JButton( Msgs.str("Reset") ); //$NON-NLS-1$
    resetBtn.addActionListener( listener );
    infoBox.add( Box.createGlue() );
    infoBox.add( resetBtn );
    
    infoMesg = new JLabel( Msgs.str("Ready"), SwingConstants.CENTER ); //$NON-NLS-1$
    infoMesg.setFont( fontLARGE );
    infoMesg.setForeground( COLOR_INFO_FRGRND );
    infoBox.add( Box.createGlue() );
    infoBox.add( infoMesg );
    infoBox.add( Box.createGlue() );
    
    infoPanel.add( infoBox );
    
  }// buildInfoPanel()
  
  /**
   * Set up the MineField and MineSettings, initial info messages, and place the game on screen and visible.
   * @see #createMineField()
   * @see #adjustSize()
   * @see #adjustScore()
   * @see MineSettings#MineSettings(MineSwinger)
   */
  private void startup()
  {
    createMineField();
    setMinesMesg( mineField.getNumHiddenMines() );
    
    adjustSize();
    adjustScore();
    
    settingsFrame = new MineSettings( this );
    
    setLocation( 630, 50 );// while in development
    setVisible( true );
    
  }// startup()

  /** create the MineField and place it in a new JLayeredPane 
   * @see MineField#MineField(MineSwinger)   */
  private void createMineField()
  {
    mineField = new MineField( this );
    mineField.setFieldLength( FIELD_SIZE_MD );
    mineField.setSquareLength( SQUARE_SIZE_MD );
    
    // set up JLayeredPane
    pane = new JLayeredPane();
    pane.add( mineField, JLayeredPane.DEFAULT_LAYER );
    getContentPane().add( pane, "Center" ); //$NON-NLS-1$
    
  }// createMineField()

  /** stop the game  */
  protected void halt()
  {
    mineField.setShaded( false ) ;
    validate();// so the last square gets painted properly
    
    mineField.removeMouseListener( mineField );
    mineField.setFirstPress( true ) ;
    
    isRunning = false ;
    gameClock.stop();
    
  }// halt()
  
  /**
   * set up a new game and repaint 
   * @see #adjustScore()
   * @see MineField#newGame()
   * @see #setMinesMesg(int)
   */
  protected void newGame()
  {
    playSound( RESET );
    
     timeTitle.setText( Msgs.str("Time")     ); //$NON-NLS-1$
      timeMesg.setText( Msgs.str("zeroTime") ); //$NON-NLS-1$
      infoMesg.setText( Msgs.str("Ready")    ); //$NON-NLS-1$
    scoreTitle.setText( Msgs.str("Score")    ); //$NON-NLS-1$
    
    adjustScore();
    currentScore = 0 ;
    scoreMesg.setText( "0" ); //$NON-NLS-1$
    
    mineField.newGame();
    minesTitle.setText( Msgs.str("mines.left") ); //$NON-NLS-1$
    setMinesMesg( mineField.getNumHiddenMines() );
    
    pane.repaint();
    
  }// newGame()
  
  /** small, medium and large -- for corresponding Square sizes  */
  private static void buildFonts()
  {
    fontSMALL  = new Font( strDEFAULT_TYPEFACE, Font.BOLD, FONT_SIZE_SM );
    fontMEDIUM = new Font( strDEFAULT_TYPEFACE, Font.BOLD, FONT_SIZE_MD );
    fontLARGE  = new Font( strDEFAULT_TYPEFACE, Font.BOLD, FONT_SIZE_LG );
    
  }// buildFonts()
  
  /**
   * get URL's from {@link #SND_CLIPS} array and load into {@link #soundFX} array
   * @see java.applet.AudioClip
   */
  void loadSounds()
  {
    URL $clip ;
    strBaseURL = "file:" + System.getProperty( "user.dir" ) //$NON-NLS-1$ //$NON-NLS-2$
                 + System.getProperty( "file.separator" );  //$NON-NLS-1$
    
    if( DEBUG_LEVEL > 0 )
      System.out.println( myname() + Msgs.str("Snd.load") + "Base URL = " + strBaseURL ); //$NON-NLS-1$ //$NON-NLS-2$
    
    try
    {
      soundFX = new java.applet.AudioClip[ NUM_SND_CLIPS ];
      
      for( int i=0; i < NUM_SND_CLIPS; i++ )
      {
        $clip = new URL( strBaseURL + SND_CLIPS[i] );
        if( DEBUG_LEVEL > 0 )
          System.out.println(
              "URL[" + i + "] = " + $clip.toString() ); //$NON-NLS-1$ //$NON-NLS-2$
        
        soundFX[i] = java.applet.Applet.newAudioClip( $clip );
      }
      soundsLoaded = true ;
    }
    catch( Exception e )
    {
      soundsLoaded = false ;
      System.err.println( myname() + Msgs.str("Snd.load") + e ); //$NON-NLS-1$
    }
    
  }// loadSounds()
  
  /** @param track - index into {@link #soundFX} array of sound to play  */
  void playSound( final int track ) 
  {
    if( !soundsActive )
      return ;
    
    if( track >= NUM_SND_CLIPS )
    {
      System.err.println( myname() + Msgs.str("Snd.play")  //$NON-NLS-1$
                          + "track #" + track  + " NOT Valid!" ); //$NON-NLS-1$ //$NON-NLS-2$
      return ;
    }
    
    if( soundFX[track] == null )
    {
      System.err.println( myname() + Msgs.str("Snd.play") + SND_CLIPS[track] + "NOT Found!" ); //$NON-NLS-1$ //$NON-NLS-2$
      return ;
    }
    
    soundFX[track].play();
    if( DEBUG_LEVEL > 1 )
      System.out.println( myname() + Msgs.str("Snd.play")  //$NON-NLS-1$
                          + "Playing '" + SND_CLIPS[track] + "'" ); //$NON-NLS-1$ //$NON-NLS-2$
    
  }// playSound()
  
  /** @param len - new length  */
  protected void newSquareLength( final int len )
  {
    // tell the mine field that Settings has changed the Square size
    mineField.setSquareLength( len );
    
    if( len == SQUARE_SIZE_SM )
    {
      mineField.setFont( fontSMALL );
    }
    else if( len == SQUARE_SIZE_LG )
    {
      mineField.setFont( fontLARGE );
    }
    else // default: should only ever be == SQUARE_SIZE_MD
        mineField.setFont( fontMEDIUM );
    
    adjustSize();
    
  }// newSquareLength()
  
  /**
   * Change the dimensions of the game frame and start a new game 
   * @see #adjustSize()
   * @see #halt()
   * @see #newGame()
   */
  protected void newSize()
  {
    adjustSize();
    halt();
    newGame();
  } 
  
  /** Adjust the size of the game frame as the <code>Square</code> size 
   *  or number of <code>Squares</code> in the field have changed */
  protected void adjustSize()
  {
    fieldDim = mineField.getSquareLength() * mineField.getFieldLength();
    setSize( fieldDim + X_BORDER*2, fieldDim + Y_BORDER*3 );
    validate();
  }
  
  /** adjust score according to current density and fieldLength  */
  protected void adjustScore()
  {
    scoreMultiplier = (int)( mineField.getDensity() * 100 );
    maxScore = scoreMultiplier * (int)Math.pow( mineField.getFieldLength(), 2.0 ) ;
  }
  
  /** @param density - new value  */
  protected void adjustDensity( final double density )
  {
    mineField.setDensity( density );
    mineField.repaint();
  }
  
  /** @return {@link #qMarksOn}  */
  boolean qMarksAreOn() { return qMarksOn ;}
  
  /** @return {@link #settingsOpen}  */
  boolean mineSettingsIsOpen() { return settingsOpen ;}
  
  /** @param state - new value for {@link #settingsOpen} */
  void setMineSettingsOpen( final boolean state ) { settingsOpen = state ;}
  
  /** start game clock and update time and info messages  */
  protected void startClock()
  {
    if( !isRunning )
    {
      isRunning = true ;
      seconds = 0 ;
      timeMesg.setText( Msgs.str("zeroTime") ); //$NON-NLS-1$
      infoMesg.setText( Msgs.str("Running")  ); //$NON-NLS-1$
      gameClock.start();
    }
  }// startClock()
  
  /** calculate and display the current elapsed game time  */
  protected void runClock()
  {
    if( isRunning && ( getState() == Frame.NORMAL ) )
    {                  // stop gameClock while game is iconified
      seconds++ ;
      
      int hrs  = seconds/3600 ; 
      int mins = seconds/60 - hrs*60 ;
      int secs = seconds - mins*60 - hrs*3600 ;
      
      String strHr  = (  hrs < 10 ? "0" : "" ) + Integer.toString( hrs  ); //$NON-NLS-1$ //$NON-NLS-2$
      String strMin = ( mins < 10 ? "0" : "" ) + Integer.toString( mins ); //$NON-NLS-1$ //$NON-NLS-2$
      String strSec = ( secs < 10 ? "0" : "" ) + Integer.toString( secs ); //$NON-NLS-1$ //$NON-NLS-2$
      
      timeMesg.setText( strHr + ":" + strMin + ":" + strSec ); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
  }// runClock()
  
  /** display text (converted from int) indicating # of mines left
   *  @param numMines - int */
  protected void setMinesMesg( final int numMines )
  {
    minesMesg.setText( " " + Integer.toString(numMines) ); //$NON-NLS-1$
  }
  
  /**
   *  calculate the game score and update the display<br>
   *  - show appropriate messages and play sounds if activated
   *  @param increase - boolean indicating if increasing or decreasing score 
   */
  protected void setScore( final boolean increase )
  {
    setMinesMesg( mineField.getNumHiddenMines() );
    
    if( increase )
    {
      currentScore += scoreMultiplier ;
      scoreMesg.setText( " " + Integer.toString(currentScore) ); //$NON-NLS-1$
      
      /* success */
      if( (currentScore == maxScore) && (mineField.getNumHiddenMines() == 0) )
      {
        double d = mineField.getDensity();
        
        minesTitle.setText( Msgs.str("mines.found") ); //$NON-NLS-1$
        minesMesg.setText( Integer.toString(mineField.getNumTotalMines()) );
        
        currentScore += ( maxScore - (100 - scoreMultiplier)*(seconds/2) );
        scoreTitle.setText( Msgs.str("Final") + Msgs.str("Score") ); //$NON-NLS-1$ //$NON-NLS-2$
        timeTitle.setText( Msgs.str("Final") + Msgs.str("Time") );   //$NON-NLS-1$ //$NON-NLS-2$
        
        if( d <= 0.02 ) // way too easy
        { 
          timeMesg.setText( Msgs.str("fullTime") );  //$NON-NLS-1$
          minesMesg.setText( "0" + Msgs.str("zd.num") ); //$NON-NLS-1$ //$NON-NLS-2$
          scoreMesg.setText( Msgs.str("zd.scr") );   //$NON-NLS-1$
          infoMesg.setText( Msgs.str("goof.info") ); //$NON-NLS-1$
        }
        else
        {
          scoreMesg.setText( " " + Integer.toString(currentScore) ); //$NON-NLS-1$
          
          if( currentScore >= maxScore * 0.5 )
          {
            if( (d > 0.0) && (d <= DEFAULT_DENSITY * 0.5) )
            {
              playSound( EASY );
              infoMesg.setText( Msgs.str("easy.info") ); //$NON-NLS-1$
            }
            else if( (d > DEFAULT_DENSITY * 0.5) && (d < DEFAULT_DENSITY) )
              {
                playSound( NEAT );     
                infoMesg.setText( Msgs.str("good.info") ); //$NON-NLS-1$
              }
            else if( (d >= DEFAULT_DENSITY) && (d < 1.0) )
              {
                playSound( AWESOME );
                infoMesg.setText( Msgs.str("great.info") ); //$NON-NLS-1$
              }
            else // d == 1.0
              { 
                infoMesg.setText( Msgs.str("goof.info") ); //$NON-NLS-1$
                scoreMesg.setText( Integer.toString((int)d) );
              }
          }
          else // currentScore < maxScore * 0.5
          {
            playSound( SLOW );
            infoMesg.setText( (currentScore > 0) ? Msgs.str("poor.info")   //$NON-NLS-1$
                                                 : Msgs.str("bad.info") ); //$NON-NLS-1$
          }
          
        }// d > 0.02
        
        mineField.paintArea(); // so the last flag is painted
        halt();
        
      }// if done
      
    }
    else // decrease
      {
        currentScore -= scoreMultiplier ;
        scoreMesg.setText( Integer.toString(currentScore) );
      }
      
  }// setScore()
  
  /** set location of MineSettings frame and make visible  */
  protected void showMineSettings()
  {
    settingsFrame.setLocation( scorePanel.getLocationOnScreen() );
    settingsFrame.setVisible( true );
  }
  
  /** @param msg - info to display  */
  protected void setInfoMesg( final String msg )
  {
    if( msg.isEmpty() )
      return;
    
    infoMesg.setText( msg );
  }    
  
  /** shut down <code>Minesweeper</code> & <code>MineSettings</code> (if open) */
  private void end()
  {
    isRunning = false ;
    
    if( mineField.exploder.isRunning() )
      mineField.exploder.stop();
    
    if( settingsOpen )
      settingsFrame.dispose();
    
  }// end()
  
  /** @return simple name of my Class  */
  String myname() { return getClass().getSimpleName(); }
  
 /*
  *     INNER CLASSES
  * ========================================================================================= */
  
  /** Handles all the action events for <code>MineSwinger</code><br>
   *  i.e. function as the Controller for the app  */
  class MineListener implements ActionListener
  {
    /** identify the source of action events */
    Object source ;
    
    /** @return simple name of my Class  */
    String myname() { return this.getClass().getSimpleName(); }
    
    @Override
    public void actionPerformed( final ActionEvent ae )
    {
      source = ae.getSource();
      if( source == gameClock )
      {
        runClock();
        if( DEBUG_LEVEL > 2 )
          System.out.println( myname() + ": Clock event" ); //$NON-NLS-1$
      }
      else // not the gameClock
      {
        if( DEBUG_LEVEL > 0 )
          System.out.println( myname() + ": Action command > " + ae.getActionCommand() ); //$NON-NLS-1$ 
        
        // NEW GAME
        if( (source == resetBtn) || (source == newGameItem) )
        {
          halt();
          newGame();
        }
        // SOUND
        else if( source == soundBtn )
        {
          if( !soundsLoaded )
          {
            loadSounds();
          }
          
          if( soundsActive )
          {
            soundsActive = false ;
            soundBtn.setText( Msgs.str("Snd.go") ); //$NON-NLS-1$
          }
          else
            {
              soundsActive = true ;
              soundBtn.setText( Msgs.str("Snd.no") ); //$NON-NLS-1$
            }
        }
        // EXIT
        else if( source == exitGameItem )
        {
          end();
          dispose();
        }
        // QMARKS
        else if( source == qMarkItem )
        {
          qMarksOn = qMarkItem.isSelected();
          
          if( !qMarksOn )
            mineField.clearQmarks();
        }
        // LAUNCH SETTINGS FRAME
        else if( source == showSettingsItem )
        {
            showMineSettings();
        }
        // MINEFIELD SIZE
        else
        {
          if( source == tinyFieldItem )
            mineField.setFieldLength( FIELD_SIZE_XS );
          else if( source == smallFieldItem )
            mineField.setFieldLength( FIELD_SIZE_SM );
          else if( source == medFieldItem )
            mineField.setFieldLength( FIELD_SIZE_MD );
          else if( source == largeFieldItem )
            mineField.setFieldLength( FIELD_SIZE_LG );
          else if( source == hugeFieldItem )
            mineField.setFieldLength( FIELD_SIZE_XL );
          
          newSize();
        }
        
      }// not the gameClock
      
    }// MineListener.actionPerformed()
    
  }/* inner class MineListener */
  
  
}/* class MineSwinger */
