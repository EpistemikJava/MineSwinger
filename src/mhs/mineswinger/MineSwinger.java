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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

/**
 * This is a Java Swing MineSweeper game inspired by the classic Windows game.
 * 
 * @author MARK SATTOLO (based on code by Mats Antell)
 * @version $Revision: #13 $
 * @see #MineSwinger(int)
 * @see #startup()
 * @see #createMineField()
 */
public class MineSwinger extends JFrame {
    /*
     *     FIELDS
     * ===================================================================================================================== */

    /** Keep the compiler from complaining... */
    private static final long serialVersionUID = 5744270357479325185L;

    final String VERSION = myname() + " $Revision: #13 $";

    static final int TIMER_DELAY_MSEC = 1000;

    static final double DEFAULT_DENSITY = 0.21;

    static final String strDEFAULT_TYPEFACE = "Arial";
    //@formatter:off
    static final Color COLOR_GAME_BKGRND = Color.cyan.darker(),
                       COLOR_SCORE_BKGRND = Color.yellow.darker(),
                       COLOR_INFO_BKGRND = Color.yellow.darker(), 
                       COLOR_INFO_FRGRND = Color.blue.darker();

    static final int FIELD_SIZE_XS = 12, // # of squares per side of the MineField
                     FIELD_SIZE_SM = 16, 
                     FIELD_SIZE_MD = 20, 
                     FIELD_SIZE_LG = 24,
                     FIELD_SIZE_XL = 30,

                     SQUARE_SIZE_SM = 18, // length in pixels of each side of an individual square
                     SQUARE_SIZE_MD = 24,
                     SQUARE_SIZE_LG = 32,

                     FONT_SIZE_SM = SQUARE_SIZE_SM / 2 + 2,
                     FONT_SIZE_MD = SQUARE_SIZE_MD / 2 + 4,
                     FONT_SIZE_LG = SQUARE_SIZE_LG / 2 + 6,

                     X_BORDER = 80, // extra space around the mine field
                     Y_BORDER = 60, // for the the labels, buttons, etc

                     BOOM = 0, 
                     MOUSE = BOOM + 1, 
                     FLAG = MOUSE + 1, 
                     NEAT = FLAG + 1, 
                     EASY = NEAT + 1,
                     AWESOME = EASY + 1,
                     RESET = AWESOME + 1,
                     BAD_CLEAR = RESET + 1, 
                     SLOW = BAD_CLEAR + 1, 
                     NUM_SND_CLIPS = SLOW + 1; // must be last entry

    // these match up with the ints in the previous list
    static final String[] SND_CLIPS = { 
                    "sounds/boom.au",
                    "sounds/mouse.au",
                    "sounds/flag.au",
                    "sounds/neat.au",
                    "sounds/easy.au",
                    "sounds/awesome.au",
                    "sounds/reset.au",
                    "sounds/bad_clear.au",
                    "sounds/slow.au"        
    };

    protected int DEBUG_LEVEL;

    static Font fontSMALL, fontMEDIUM, fontLARGE;

    private boolean isRunning = false, 
                    settingsOpen = false, 
                    qMarksOn = false;

    private int seconds, // accumulated game time
                scoreMultiplier, // increase scoring according to density, etc
                maxScore, 
                currentScore = 0;

    /**
     * length, in pixels, of each side of the <code>MineField</code><br>
     * = (current side length of each square) * (# of squares on each side of field)
     */
    private int fieldDim;

    private boolean soundsActive = false;

    MineField mineField;
    MineSettings settingsFrame;

    private JLayeredPane pane;
    private MineListener listener;
    private Timer gameClock;

    private KeyStroke ks;
    private JMenuBar gameBar;
    private JMenu gameMenu, settingsMenu;
    private JMenuItem tinyFieldItem, smallFieldItem, medFieldItem, largeFieldItem, hugeFieldItem,
                      exitGameItem, newGameItem, showSettingsItem;

    private JCheckBoxMenuItem qMarkItem;

    private JPanel scorePanel, infoPanel;
    private JButton soundBtn, resetBtn;
    private Box infoBox;

    private JLabel infoMesg, timeMesg, scoreMesg, minesMesg;
    private JLabel timeTitle, scoreTitle, minesTitle;
    //@formatter:on
    /*
     *     METHODS
     * ===================================================================================================================== */

    /**
     * MAIN
     * @param args - from command line
     * @see MineSwinger#MineSwinger(int)
     * @see MineSwinger#startup()
     */
    public static void main(final String[] args) {
        System.out.println( "PROGRAM STARTED ON " + Thread.currentThread() );
        SwingUtilities.invokeLater( new Runnable() {
            @Override
            public void run() {
                new MineSwinger( args.length > 0 ? Integer.parseInt( args[0] ) : 0 ).startup();
            }
        } );
    }

    /**
     * CONSTRUCTOR
     * @param debug level
     * @see #buildFonts()
     * @see #buildComponents()
     */
    public MineSwinger(final int debug) {
        DEBUG_LEVEL = debug;
        System.out.println( myname() + Msgs.str( "debug_level" ) + DEBUG_LEVEL );

        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        setTitle( VERSION );
        getContentPane().setBackground( COLOR_GAME_BKGRND );

        listener = new MineListener();
        gameClock = new Timer( TIMER_DELAY_MSEC, listener );

        buildFonts();
        buildComponents();

        setFocusable( true );
        setResizable( false );
    }

    /**
     * build and set the JMenuBar; build the Panels and set in the ContentPane
     * @see #buildJMenuBar()
     * @see #buildScorePanel()
     * @see #buildInfoPanel()
     */
    private void buildComponents() {
        buildJMenuBar();
        setJMenuBar( gameBar );

        buildScorePanel();
        buildInfoPanel();

        getContentPane().add( scorePanel, "North" );
        getContentPane().add( infoPanel, "South" );
    }

    /**
     * build GameMenu and SettingsMenu and set in the JMenuBar
     * @see #buildGameMenu()
     * @see #buildSettingsMenu()
     */
    private void buildJMenuBar() {
        // create Menu Bar
        gameBar = new JMenuBar();

        buildGameMenu();
        buildSettingsMenu();

        gameBar.add( gameMenu );
        gameBar.add( settingsMenu );
    }

    /** contains items for choosing Square size and for exiting the game */
    private void buildGameMenu() {
        gameMenu = new JMenu( Msgs.str( "Game" ) );

        tinyFieldItem = new JMenuItem( Msgs.str( "field.xs" ) + FIELD_SIZE_XS + Msgs.str( "sqr_side" ) );
        ks = KeyStroke.getKeyStroke( KeyEvent.VK_T, Event.ALT_MASK );
        tinyFieldItem.setAccelerator( ks );
        tinyFieldItem.setMnemonic( 'T' );
        tinyFieldItem.addActionListener( listener );

        smallFieldItem = new JMenuItem( Msgs.str( "field.sm" ) + FIELD_SIZE_SM + Msgs.str( "sqr_side" ) );
        ks = KeyStroke.getKeyStroke( KeyEvent.VK_S, Event.ALT_MASK );
        smallFieldItem.setAccelerator( ks );
        smallFieldItem.setMnemonic( 'S' );
        smallFieldItem.addActionListener( listener );

        medFieldItem = new JMenuItem( Msgs.str( "field.md" ) + FIELD_SIZE_MD + Msgs.str( "sqr_side" ) );
        ks = KeyStroke.getKeyStroke( KeyEvent.VK_M, Event.ALT_MASK );
        medFieldItem.setAccelerator( ks );
        medFieldItem.setMnemonic( 'M' );
        medFieldItem.addActionListener( listener );

        largeFieldItem = new JMenuItem( Msgs.str( "field.lg" ) + FIELD_SIZE_LG + Msgs.str( "sqr_side" ) );
        ks = KeyStroke.getKeyStroke( KeyEvent.VK_L, Event.ALT_MASK );
        largeFieldItem.setAccelerator( ks );
        largeFieldItem.setMnemonic( 'L' );
        largeFieldItem.addActionListener( listener );

        hugeFieldItem = new JMenuItem( Msgs.str( "field.xl" ) + FIELD_SIZE_XL + Msgs.str( "sqr_side" ) );
        ks = KeyStroke.getKeyStroke( KeyEvent.VK_H, Event.ALT_MASK );
        hugeFieldItem.setAccelerator( ks );
        hugeFieldItem.setMnemonic( 'H' );
        hugeFieldItem.addActionListener( listener );

        exitGameItem = new JMenuItem( Msgs.str( "Exit" ) );
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
    }

    /** contains items to activate question marks, start a new game, and opening the MineSettings frame */
    private void buildSettingsMenu() {
        settingsMenu = new JMenu( Msgs.str( "Settings" ) );

        qMarkItem = new JCheckBoxMenuItem( Msgs.str( "Qmarks" ), qMarksOn );
        ks = KeyStroke.getKeyStroke( KeyEvent.VK_Q, Event.ALT_MASK );
        qMarkItem.setAccelerator( ks );
        qMarkItem.setMnemonic( 'Q' );
        qMarkItem.addActionListener( listener );

        newGameItem = new JMenuItem( Msgs.str( "game.new" ) );
        ks = KeyStroke.getKeyStroke( KeyEvent.VK_F2, 0 );
        newGameItem.setAccelerator( ks );
        newGameItem.addActionListener( listener );

        showSettingsItem = new JMenuItem( Msgs.str( "settings.change" ) );
        ks = KeyStroke.getKeyStroke( KeyEvent.VK_C, Event.ALT_MASK );
        showSettingsItem.setAccelerator( ks );
        showSettingsItem.setMnemonic( 'C' );
        showSettingsItem.addActionListener( listener );

        settingsMenu.add( qMarkItem );
        settingsMenu.add( newGameItem );
        settingsMenu.add( showSettingsItem );
    }

    /** displays the current score, number of mines left (or found), and elapsed time */
    private void buildScorePanel() {
        scorePanel = new JPanel( new GridLayout( 1, 6 ) );
        scorePanel.setBackground( COLOR_SCORE_BKGRND );

        scoreTitle = new JLabel( Msgs.str( "Score" ) );
        scoreTitle.setHorizontalAlignment( SwingConstants.CENTER );
        scoreMesg = new JLabel( "0" );
        scorePanel.add( scoreTitle );
        scorePanel.add( scoreMesg );

        minesTitle = new JLabel( Msgs.str( "mines.left" ) );
        minesMesg = new JLabel( "0" );
        scorePanel.add( minesTitle );
        scorePanel.add( minesMesg );

        timeTitle = new JLabel( Msgs.str( "Time" ) );
        timeMesg = new JLabel( Msgs.str( "zeroTime" ) );
        scorePanel.add( timeTitle );
        scorePanel.add( timeMesg );
    }

    /**
     * contains the 'Activate/De-activate Sounds' button, the 'Reset' button;<br>
     * -- also displays useful game info to the user, e.g. 'Ready'
     */
    private void buildInfoPanel() {
        infoPanel = new JPanel( new GridLayout( 1, 1 ) );
        infoBox = Box.createHorizontalBox();

        // setBackground() does NOT work with a Box
        infoPanel.setBackground( COLOR_INFO_BKGRND );

        soundBtn = new JButton( Msgs.str( "Snd.go" ) );
        soundBtn.addActionListener( listener );
        infoBox.add( Box.createGlue() );
        infoBox.add( soundBtn );

        resetBtn = new JButton( Msgs.str( "Reset" ) );
        resetBtn.addActionListener( listener );
        infoBox.add( Box.createGlue() );
        infoBox.add( resetBtn );

        infoMesg = new JLabel( Msgs.str( "Ready" ), SwingConstants.CENTER );
        infoMesg.setFont( fontLARGE );
        infoMesg.setForeground( COLOR_INFO_FRGRND );
        infoBox.add( Box.createGlue() );
        infoBox.add( infoMesg );
        infoBox.add( Box.createGlue() );

        infoPanel.add( infoBox );
    }

    /**
     * Set up the MineField and MineSettings, initial info messages, and place the game on screen and visible.
     * @see #createMineField()
     * @see #adjustSize()
     * @see #adjustScore()
     * @see MineSettings#MineSettings(MineSwinger)
     */
    private void startup() {
        createMineField();
        setMinesMesg( mineField.getNumHiddenMines() );

        adjustSize();
        adjustScore();

        settingsFrame = new MineSettings( this );
        setLocation( 630, 50 );// while in development
        setVisible( true );
    }

    /**
     * create the MineField and place it in a new JLayeredPane
     * @see MineField#MineField(MineSwinger)
     */
    private void createMineField() {
        mineField = new MineField( this );
        mineField.setFieldLength( FIELD_SIZE_MD );
        mineField.setSquareLength( SQUARE_SIZE_MD );

        // set up JLayeredPane
        pane = new JLayeredPane();
        pane.add( mineField, JLayeredPane.DEFAULT_LAYER );
        getContentPane().add( pane, "Center" );
    }

    /** stop the game */
    protected void halt() {
        mineField.setShaded( false );
        validate();// so the last square gets painted properly

        mineField.removeMouseListener( mineField );
        mineField.setFirstPress( true );

        isRunning = false;
        gameClock.stop();
    }

    /**
     * set up a new game and repaint
     * @see #adjustScore()
     * @see MineField#newGame()
     * @see #setMinesMesg(int)
     */
    protected void newGame() {
        playSound( RESET );

        timeTitle.setText( Msgs.str( "Time" ) );
        timeMesg.setText( Msgs.str( "zeroTime" ) );
        infoMesg.setText( Msgs.str( "Ready" ) );
        scoreTitle.setText( Msgs.str( "Score" ) );

        adjustScore();
        currentScore = 0;
        scoreMesg.setText( "0" );

        mineField.newGame();
        minesTitle.setText( Msgs.str( "mines.left" ) );
        setMinesMesg( mineField.getNumHiddenMines() );

        pane.repaint();
    }

    /** small, medium and large -- for corresponding Square sizes */
    private static void buildFonts() {
        fontSMALL = new Font( strDEFAULT_TYPEFACE, Font.BOLD, FONT_SIZE_SM );
        fontMEDIUM = new Font( strDEFAULT_TYPEFACE, Font.BOLD, FONT_SIZE_MD );
        fontLARGE = new Font( strDEFAULT_TYPEFACE, Font.BOLD, FONT_SIZE_LG );
    }

    /** @param track - index into {@link #SND_CLIPS} array of sound to play */
    void playSound(final int track) {
        if( !soundsActive ) return;

        if( track >= NUM_SND_CLIPS ) {
            System.err.println( myname() + Msgs.str( "Snd.play" ) + "track #" + track + " NOT Valid!" );
            return;
        }

        if( SND_CLIPS[track] == null ) {
            System.err.println( myname() + Msgs.str( "Snd.play" ) + SND_CLIPS[track] + "NOT Found!" );
            return;
        }

        // TODO Separate thread?
        // open the sound file as a Java input stream
        InputStream in;
        try {
            in = new FileInputStream( System.getProperty( "user.dir" ) + System.getProperty( "file.separator" )
                            + SND_CLIPS[track] );
            // create an audiostream from the inputstream
            AudioStream audioStream = new AudioStream( in );
            // play the audio clip with the audioplayer class
            AudioPlayer.player.start( audioStream );
        } catch( FileNotFoundException fnfe ) {
            System.err.println( myname() + Msgs.str( "Snd.play" ) + SND_CLIPS[track] + "NOT Found:" + fnfe );
        } catch( IOException ioe ) {
            System.err.println( myname() + Msgs.str( "Snd.play" ) + SND_CLIPS[track] + "IO Problem: " + ioe );
        }
        if( DEBUG_LEVEL > 1 ) System.out.println( myname() + Msgs.str( "Snd.play" ) + "Playing '" + SND_CLIPS[track] + "'" );
    }

    /** @param len - new length */
    protected void newSquareLength(final int len) {
        // tell the mine field that Settings has changed the Square size
        mineField.setSquareLength( len );

        if( len == SQUARE_SIZE_SM )
            mineField.setFont( fontSMALL );
        else if( len == SQUARE_SIZE_LG )
            mineField.setFont( fontLARGE );
        else
            // default: should only ever be == SQUARE_SIZE_MD
            mineField.setFont( fontMEDIUM );

        adjustSize();
    }

    /**
     * Change the dimensions of the game frame and start a new game
     * @see #adjustSize()
     * @see #halt()
     * @see #newGame()
     */
    protected void newSize() {
        adjustSize();
        halt();
        newGame();
    }

    /**
     * Adjust the size of the game frame as the <code>Square</code> size or number of <code>Squares</code> in the field have
     * changed
     */
    protected void adjustSize() {
        fieldDim = mineField.getSquareLength() * mineField.getFieldLength();
        setSize( fieldDim + X_BORDER * 2, fieldDim + Y_BORDER * 3 );
        validate();
    }

    /** adjust score according to current density and fieldLength */
    protected void adjustScore() {
        scoreMultiplier = (int) (mineField.getDensity() * 100);
        maxScore = scoreMultiplier * (int) Math.pow( mineField.getFieldLength(), 2.0 );
    }

    /** @param density - new value */
    protected void adjustDensity(final double density) {
        mineField.setDensity( density );
        mineField.repaint();
    }

    /** @return {@link #qMarksOn} */
    boolean qMarksAreOn() {
        return qMarksOn;
    }

    /** @return {@link #settingsOpen} */
    boolean mineSettingsIsOpen() {
        return settingsOpen;
    }

    /** @param state - new value for {@link #settingsOpen} */
    void setMineSettingsOpen(final boolean state) {
        settingsOpen = state;
    }

    /** start game clock and update time and info messages */
    protected void startClock() {
        if( !isRunning ) {
            isRunning = true;
            seconds = 0;
            timeMesg.setText( Msgs.str( "zeroTime" ) );
            infoMesg.setText( Msgs.str( "Running" ) );
            gameClock.start();
        }
    }

    /** calculate and display the current elapsed game time */
    protected void runClock() {
        // stop gameClock while game is iconified
        if( isRunning && (getState() == Frame.NORMAL) ) {
            seconds++;

            int hrs = seconds / 3600;
            int mins = seconds / 60 - hrs * 60;
            int secs = seconds - mins * 60 - hrs * 3600;

            String strHr = (hrs < 10 ? "0" : "") + Integer.toString( hrs );
            String strMin = (mins < 10 ? "0" : "") + Integer.toString( mins );
            String strSec = (secs < 10 ? "0" : "") + Integer.toString( secs );

            timeMesg.setText( strHr + ":" + strMin + ":" + strSec );
        }
    }

    /**
     * display text (converted from int) indicating # of mines left
     * @param numMines - int
     */
    protected void setMinesMesg(final int numMines) {
        minesMesg.setText( " " + Integer.toString( numMines ) );
    }

    /**
     * calculate the game score and update the display<br>
     * - show appropriate messages and play sounds if activated
     * @param increase - boolean indicating if increasing or decreasing score
     */
    protected void setScore(final boolean increase) {
        setMinesMesg( mineField.getNumHiddenMines() );

        if( increase ) {
            currentScore += scoreMultiplier;
            scoreMesg.setText( " " + Integer.toString( currentScore ) );

            /* success */
            if( (currentScore == maxScore) && (mineField.getNumHiddenMines() == 0) ) {
                double d = mineField.getDensity();

                minesTitle.setText( Msgs.str( "mines.found" ) );
                minesMesg.setText( Integer.toString( mineField.getNumTotalMines() ) );

                currentScore += (maxScore - (100 - scoreMultiplier) * (seconds / 2));
                scoreTitle.setText( Msgs.str( "Final" ) + Msgs.str( "Score" ) );
                timeTitle.setText( Msgs.str( "Final" ) + Msgs.str( "Time" ) );

                if( d <= 0.02 ) // way too easy
                {
                    timeMesg.setText( Msgs.str( "fullTime" ) );
                    minesMesg.setText( "0" + Msgs.str( "zd.num" ) );
                    scoreMesg.setText( Msgs.str( "zd.scr" ) );
                    infoMesg.setText( Msgs.str( "goof.info" ) );
                } else {
                    scoreMesg.setText( " " + Integer.toString( currentScore ) );

                    if( currentScore >= maxScore * 0.5 ) {
                        if( (d > 0.0) && (d <= DEFAULT_DENSITY * 0.5) ) {
                            playSound( EASY );
                            infoMesg.setText( Msgs.str( "easy.info" ) );
                        } else if( (d > DEFAULT_DENSITY * 0.5) && (d < DEFAULT_DENSITY) ) {
                            playSound( NEAT );
                            infoMesg.setText( Msgs.str( "good.info" ) );
                        } else if( (d >= DEFAULT_DENSITY) && (d < 1.0) ) {
                            playSound( AWESOME );
                            infoMesg.setText( Msgs.str( "great.info" ) );
                        } else // d == 1.0
                        {
                            infoMesg.setText( Msgs.str( "goof.info" ) );
                            scoreMesg.setText( Integer.toString( (int) d ) );
                        }
                    } else // currentScore < maxScore * 0.5
                    {
                        playSound( SLOW );
                        infoMesg.setText( (currentScore > 0) ? Msgs.str( "poor.info" ) : Msgs.str( "bad.info" ) );
                    }
                }// else d > 0.02

                mineField.paintArea(); // so the last flag is painted
                halt();
            }// if game over
        } else // decrease
        {
            currentScore -= scoreMultiplier;
            scoreMesg.setText( Integer.toString( currentScore ) );
        }
    }

    /** set location of MineSettings frame and make visible */
    protected void showMineSettings() {
        settingsFrame.setLocation( scorePanel.getLocationOnScreen() );
        settingsFrame.setVisible( true );
    }

    /** @param msg - info to display */
    protected void setInfoMesg(final String msg) {
        if( msg.isEmpty() ) //
            return;
        infoMesg.setText( msg );
    }

    /** shut down <code>Minesweeper</code> & <code>MineSettings</code> (if open) */
    private void end() {
        isRunning = false;
        if( mineField.exploder.isRunning() ) mineField.exploder.stop();
        if( settingsOpen ) settingsFrame.dispose();
    }

    /** @return simple name of my Class */
    String myname() {
        return getClass().getSimpleName();
    }

    /*
     *     INNER CLASSES
     * ===================================================================================================================== */

    /**
     * Handles all the action events for <code>MineSwinger</code><br>
     * i.e. function as the Controller for the app
     */
    class MineListener implements ActionListener {
        /** identify the source of action events */
        Object source;

        /** @return simple name of my Class */
        String myname() {
            return this.getClass().getSimpleName();
        }

        @Override
        public void actionPerformed(final ActionEvent ae) {
            source = ae.getSource();
            if( source == gameClock ) {
                runClock();
                if( DEBUG_LEVEL > 2 ) System.out.println( myname() + ": Clock event" );
            } else // not the gameClock
            {
                if( DEBUG_LEVEL > 0 ) System.out.println( myname() + ": Action command > " + ae.getActionCommand() );

                // NEW GAME
                if( (source == resetBtn) || (source == newGameItem) ) {
                    halt();
                    newGame();
                }
                // SOUND
                else if( source == soundBtn ) {
                    if( soundsActive ) {
                        soundsActive = false;
                        soundBtn.setText( Msgs.str( "Snd.go" ) );
                    } else {
                        soundsActive = true;
                        soundBtn.setText( Msgs.str( "Snd.no" ) );
                    }
                }
                // EXIT
                else if( source == exitGameItem ) {
                    end();
                    dispose();
                }
                // QMARKS
                else if( source == qMarkItem ) {
                    qMarksOn = qMarkItem.isSelected();

                    if( !qMarksOn ) mineField.clearQmarks();
                }
                // LAUNCH SETTINGS FRAME
                else if( source == showSettingsItem ) {
                    showMineSettings();
                }
                // MINEFIELD SIZE
                else {
                    if( source == tinyFieldItem )
                        mineField.setFieldLength( FIELD_SIZE_XS );
                    else if( source == smallFieldItem )
                        mineField.setFieldLength( FIELD_SIZE_SM );
                    else if( source == medFieldItem )
                        mineField.setFieldLength( FIELD_SIZE_MD );
                    else if( source == largeFieldItem )
                        mineField.setFieldLength( FIELD_SIZE_LG );
                    else if( source == hugeFieldItem ) mineField.setFieldLength( FIELD_SIZE_XL );

                    newSize();
                }
            }// not the gameClock
        }

    }/* inner class MineListener */

}/* class MineSwinger */
