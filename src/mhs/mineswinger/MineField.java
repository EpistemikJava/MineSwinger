/* ******************************************************************************
 * 
 *  Mark Sattolo (epistemik@gmail.com)
 * -----------------------------------------------
 *  $File: //depot/Eclipse/Java/workspace/MineSwinger/src/mhs/mineswinger/MineField.java $
 *  $Revision: #13 $
 *  $Change: 155 $
 *  $DateTime: 2011/12/25 00:33:16 $
 * -----------------------------------------------
 * 
 * MineField.java
 * Created on Jan 29, 2008, 19h24
 * Initial git version created Apr 12, 2015
 * 
 ********************************************************************************/

package mhs.mineswinger;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * MineField contains the game content
 * 
 * @author MARK SATTOLO (based on code by Mats Antell)
 * @version $Revision: #13 $
 */
class MineField extends JLabel implements MouseListener {
    /*
     *     FIELDS
     * ===================================================================================================================== */

    /** Keep the compiler from complaining... */
    private static final long serialVersionUID = -9210718514352269566L;
    //@formatter:off
    static final boolean PLUS = true, // indicate if score should increase
                         MINUS = false; // or decrease

    /** # of animation 'frames' in the explosion */
    static final int EXPLODE_INDEX = 9;
    /** pause (msec) between each explosion 'frame' */
    static final int EXPLODE_DELAY = 120;

    /** #s 1-8 are for <code>Squares</code> adjacent to mines */
    static final int MINE = 9, 
                     FLAG = 10, 
                     QMARK = 11, 
                     BLANK = 12, 
                     BADCLEAR = 13, 
                     FATAL = 14;

    static final Color
                    COLOR_BRIGHT = Color.white, COLOR_BLAZE = Color.yellow, COLOR_BLAST = Color.orange,
                    COLOR_BOLD = Color.red, COLOR_SHADE = Color.pink, COLOR_LIGHT = Color.lightGray, COLOR_MEDIUM = Color.gray,
                    COLOR_DARK = Color.darkGray, COLOR_DARKEST = Color.black, COLOR_BRUN = new Color( 107, 69, 38 ),

                    COLOR_FLAG_BKGRND = Color.green.brighter(), COLOR_FLAG_BELT = Color.red.darker(),
                    COLOR_FLAG_BURST = Color.yellow,

                    COLOR_NUM1 = Color.blue, COLOR_NUM2 = Color.green.darker().darker(), COLOR_NUM3 = Color.red,
                    COLOR_NUM4 = Color.black, COLOR_NUM5 = Color.magenta, COLOR_NUM6 = Color.yellow, COLOR_NUM7 = Color.cyan,
                    COLOR_NUM8 = Color.orange.darker();

    private int DEBUG_LEVEL;

    private double density = MineSwinger.DEFAULT_DENSITY;

    /** number of mines remaining to be flagged in the current game */
    private int hiddenMines;
    /** total number of mines, hidden or flagged, set in the current game */
    private int totalMines;

    /** number of <code>Squares</code> on each side of the 2D mine array */
    private int fieldLength;
    /** length of sides (in pixels) of each individual <code>Square</code> */
    private int squareLength;

    private int destx, desty, // x and y co-ordinates for a Square to be destroyed
                paintLeft, paintRight, paintTop, paintBottom; // Squares to be painted adjacent to the current active Square

    /** reference to the enclosing class */
    private MineSwinger gameview;

    /** 2D array of individual mine <code>Squares</code> */
    private Square[][] mine2dArray;

    private boolean firstScan, destroyed, cleared, badClear,
                    firstPress = true, paintAll = true,
                    lightup = false, shaded = false;
    //@formatter:on
    /**
     * generates action events used by the <code>ExplodeListener</code>
     * @see MineField#listener
     * @see Timer
     */
    Timer exploder;

    /**
     * inner class that calls <code>repaint()</code> to produce the animation explosion
     * @see ExplodeListener
     */
    private ExplodeListener listener;

    /*
     *     METHODS
     * ===================================================================================================================== */

    /**
     * CONSTRUCTOR <br>
     * called ONLY by <code>MineSwinger.createMineField()</code>
     * @param game - reference to the enclosing {@link MineSwinger}
     */
    MineField(final MineSwinger game) {
        DEBUG_LEVEL = game.DEBUG_LEVEL;
        if( DEBUG_LEVEL > 0 ) System.out.println( myname() + Msgs.str( "debug_level" ) + DEBUG_LEVEL );

        gameview = game;
        squareLength = MineSwinger.FIELD_SIZE_MD;
        reset( MineSwinger.FIELD_SIZE_MD );

        setFont( MineSwinger.fontMEDIUM );
        setLocation( MineSwinger.X_BORDER, MineSwinger.Y_BORDER / 2 );

        listener = new ExplodeListener();
        exploder = new Timer( EXPLODE_DELAY, listener );
    }

    /** @return simple name of my Class */
    String myname() {
        return this.getClass().getSimpleName();
    }

    /**
     * Reset the mine array
     * @see #reset(int)
     */
    void newGame() {
        firstPress = true;
        gameview.validate();
        if( exploder.isRunning() ) exploder.stop();
        reset( fieldLength );
    }

    /**
     * Set the initial variables
     * @param len - field length
     * @see #layMines()
     * @see #count()
     */
    private void reset(int len) {
        fieldLength = len;
        setSize( fieldLength * squareLength, fieldLength * squareLength );

        hiddenMines = 0;
        totalMines = (int) (density * fieldLength * fieldLength);
        // make the laying mines loop easier in case of zero density
        if( totalMines < 1 ) {
            totalMines = 1;
            density = 1.0 / (fieldLength * fieldLength);
        }
        System.out.println( "totalMines = " + totalMines );
        System.out.println( "density = " + density );

        layMines();
        cleared = badClear = destroyed = false;
        count();

        setBackground( COLOR_DARK );
        addMouseListener( this );
    }

    //@formatter:off
    /**
     * have <code>MineSwinger</code> play a sound
     * @param snd - index in soundClip array of sound to play
     * @see MineSwinger#playSound(int)
     */
    void playSound(final int snd) { gameview.playSound( snd );}

    /**
     * have <code>MineSwinger</code> adjust the score
     * @param plus - increase or decrease the score
     * @see MineSwinger#setScore(boolean)
     */
    void setScore(final boolean plus) { gameview.setScore( plus );}

    /**
     * have <code>MineSwinger</code> display a message
     * @param info to display
     */
    void setGameText(final String info) { gameview.setInfoMesg( info );}

    /**
     * update the display of the number of mines remaining
     * @param num - number of mines remaining
     */
    void setMinesText(final int num) { gameview.setMinesMesg( num );}

    /** @return remaining number of hidden mines in the current game */
    int getNumHiddenMines() { return hiddenMines;}

    /** @return total number of mines, hidden or flagged, in the current game */
    int getNumTotalMines() { return totalMines;}

    /** @return {@link #shaded} */
    boolean isShaded() { return shaded;}

    /** @param state - new value for {@link #shaded} */
    void setShaded(final boolean state) { shaded = state;}

    /** @return {@link #firstPress} */
    boolean isFirstPress() { return firstPress;}

    /** @param state - new value for {@link #firstPress} */
    void setFirstPress(final boolean state) { firstPress = state;}

    /** @return density */
    double getDensity() { return density;}

    /**
     * change current mine density
     * @param dens - to set
     */
    void setDensity(final double dens) { density = dens;}

    /** @return {@link #fieldLength} */
    int getFieldLength() { return fieldLength;}

    /** @return {@link #squareLength} */
    int getSquareLength() { return squareLength;}
    //@formatter:on
    /**
     * change array length (number of <code>Squares</code> per side) and adjust overall array size
     * @param len - new field length
     * @return previous field length
     */
    int setFieldLength(final int len) {
        int $oldlen = fieldLength;
        fieldLength = len;
        setSize( squareLength * fieldLength, squareLength * fieldLength );
        return $oldlen;
    }

    /**
     * change length (pixels) of each individual <code>Square</code> and adjust overall array size
     * @param side - square length
     */
    void setSquareLength(final int side) {
        squareLength = side;
        setSize( squareLength * fieldLength, squareLength * fieldLength );
    }

    /** Clear any active question marks if user has de-activated them */
    protected void clearQmarks() {
        boolean $haveQmarks = false;
        int i, j;

        for( i = 0; i < fieldLength; i++ )
            for( j = 0; j < fieldLength; j++ ) {
                if( mine2dArray[i][j].hasQmark() ) {
                    if( DEBUG_LEVEL > 1 )
                        System.out.println( myname() + Msgs.str( "Qmarks.clear" ) + "Square[" + i + "][" + j + "] "
                                        + Msgs.str( "Qmark.has" ) );

                    mine2dArray[i][j].setQmark( false );
                    $haveQmarks = true;
                }
            }
        if( $haveQmarks ) repaint();
    }

    /** Randomly seed the field with mines based on the current density */
    protected void layMines() {
        int i, j;
        boolean $fillin = false;
        mine2dArray = new Square[fieldLength][fieldLength];

        while( hiddenMines < totalMines ) {
            for( i = 0; i < fieldLength; i++ )
                for( j = 0; j < fieldLength; j++ ) {
                    if( !$fillin ) mine2dArray[i][j] = new Square();

                    if( hiddenMines < totalMines ) {
                        if( (!mine2dArray[i][j].hasMine) && (Math.random() <= density) ) {
                            mine2dArray[i][j].arm();
                            hiddenMines++;
                        }
                    }
                }
            $fillin = true;
        }
        System.out.println( "hiddenMines = " + hiddenMines );
    }

    /**
     * Set the minecount variable of each <code>Square</code>.<br>
     * - this is the total number of mines in all neighbouring <code>Squares</code>
     */
    protected void count() {
        int u, v, i, j, $detected = 0;

        for( i = 0; i < fieldLength; i++ )
            for( j = 0; j < fieldLength; j++, $detected = 0 ) {
                // co-ordinates of the 3x3 (or smaller if near an edge) grid containing all the adjacent Squares
                int left = (i == 0 ? 0 : i - 1);
                int right = (i == fieldLength - 1 ? i : i + 1);
                int top = (j == 0 ? 0 : j - 1);
                int bottom = (j == fieldLength - 1 ? j : j + 1);

                for( u = left; u <= right; u++ )
                    for( v = top; v <= bottom; v++ )
                        if( mine2dArray[u][v].hasMine() ) $detected++;

                mine2dArray[i][j].setMinecount( $detected );
            }
    }

    /**
     * RECURSIVELY find and reveal all adjacent blank and numbered <code>Squares</code><br>
     * - gets called <bold>recursively</bold> for blank <code>Squares</code> (i.e. those NOT adjacent to any mines)
     * @param u - horizontal co-ordinate
     * @param v - vertical co-ordinate
     */
    protected void clearOut(final int u, final int v) {
        if( !mine2dArray[u][v].isRevealed() ) mine2dArray[u][v].setRevealed();

        /* have to reset these each time because this is a recursive method */
        int $left = (u == 0 ? 0 : u - 1);
        int $right = (u == fieldLength - 1 ? u : u + 1);
        int $top = (v == 0 ? 0 : v - 1);
        int $bot = (v == fieldLength - 1 ? v : v + 1);

        if( firstScan ) {
            firstScan = false;
            paintLeft = $left;
            paintRight = $right;
            paintTop = $top;
            paintBottom = $bot;
        } else {
            if( $left < paintLeft ) paintLeft = $left;
            if( $right > paintRight ) paintRight = $right;
            if( $top < paintTop ) paintTop = $top;
            if( $bot > paintBottom ) paintBottom = $bot;
        }

        int i, j;
        for( i = $left; i <= $right; i++ )
            for( j = $top; j <= $bot; j++ )
                if( !mine2dArray[i][j].isRevealed() ) {
                    mine2dArray[i][j].setRevealed();
                    setScore( PLUS );
                    // recursive call if any adjacent squares are also blank
                    if( mine2dArray[i][j].isBlank() ) clearOut( i, j );
                }
    }

    /** @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent) */
    public void mouseClicked(MouseEvent me) {}

    /** @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent) */
    public void mouseEntered(MouseEvent me) {}

    /** @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent) */
    public void mouseExited(MouseEvent me) {}

    /**
     * Set a flag, or reveal a <code>Square</code>, or clear out an area, etc.<br>
     * - handles all user input from the various mouse buttons
     * @param evt - {@link MouseEvent}
     */
    @Override
    public void mousePressed(final MouseEvent evt) {
        int $flagcount, $minecount, $revealcount, i, j;

        int x = evt.getX() / squareLength;
        int y = evt.getY() / squareLength;
        if( DEBUG_LEVEL > 2 ) System.out.println( myname() + Msgs.str( "mouse.press" ) + "Square[" + x + "][" + y + "] " );

        Square activeSqr = mine2dArray[x][y];
        if( firstPress ) gameview.startClock();

        // TODO: need this?
        if( cleared ) return; // prevent a 2nd press from erasing the clearOut() dimensions

        // Need at least Java version 1.4 to use getModifiersEx()
        int pressed = evt.getModifiersEx();
        if( DEBUG_LEVEL > 1 )
            System.out.println( myname() + Msgs.str( "mouse.press" ) + InputEvent.getModifiersExText( pressed ) );

        paintLeft = paintRight = x;
        paintTop = paintBottom = y;

        /* already Revealed, i.e. number or blank
         * button 1 = light up
         * button 2 = null
         * button 3 = clear out  */
        if( activeSqr.isRevealed() ) {
            // BUTTON 1 CLICKED
            if( (pressed & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK ) {
                // light up the background
                lightup = true;
                paintArea();
            } else
            // BUTTON 3 CLICKED
            if( (pressed & InputEvent.BUTTON3_DOWN_MASK) == InputEvent.BUTTON3_DOWN_MASK )
            // clear around the square
            // - if the square is numbered and all adjacent mines are properly flagged,
            // will reveal all adjacent squares - including showing ALL attached blank space
            {
                if( activeSqr.isBlank() ) return;

                lightup = shaded = false;
                $flagcount = $revealcount = 0;
                $minecount = activeSqr.getMinecount();

                int left = (x == 0 ? 0 : x - 1);
                int right = (x == fieldLength - 1 ? x : x + 1);
                int top = (y == 0 ? 0 : y - 1);
                int bottom = (y == fieldLength - 1 ? y : y + 1);

                for( i = left; i <= right; i++ )
                    for( j = top; j <= bottom; j++ ) {
                        if( mine2dArray[i][j].hasFlag() ) {
                            $flagcount++;
                            /* game over if try to clear in vicinity of an incorrect flag */
                            if( !mine2dArray[i][j].hasMine() ) {
                                playSound( MineSwinger.BAD_CLEAR );
                                setGameText( Msgs.str( "badclear.info" ) );
                                paintLeft = i;
                                paintTop = j;
                                badClear = true;
                                return;
                            }
                        }
                        if( mine2dArray[i][j].isRevealed() ) $revealcount++;
                    }

                // how many squares in our adjacency grid?
                int gridsize = (right - left + 1) * (bottom - top + 1);
                // all adjacent squares except mines are already revealed
                boolean opengrid = ($revealcount == (gridsize - $minecount));

                if( ($flagcount == $minecount) && !opengrid ) {
                    paintLeft = left;
                    paintRight = right;
                    paintTop = top;
                    paintBottom = bottom;

                    for( i = left; i <= right; i++ )
                        for( j = top; j <= bottom; j++ )
                            if( !mine2dArray[i][j].hasMine() && !mine2dArray[i][j].isRevealed() ) {
                                mine2dArray[i][j].setRevealed();
                                setScore( PLUS );
                                if( mine2dArray[i][j].isBlank() ) clearOut( i, j );
                            }
                }
                playSound( MineSwinger.MOUSE );

            } // BUTTON 3 CLICKED

        } // Revealed Square
        else
        /* Square NOT Revealed, i.e. normal (hidden) or flag or qmark
         * button 1 = reveal for normal, null for flag or qmark
         * button 2 = qmark (if active) for normal, null for flag, remove qmark if one is present
         * button 3 = set flag for normal or qmark, remove flag is one is present  */
        {
            // BUTTON 1 CLICKED
            if( (pressed & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK ) {
                if( activeSqr.hasFlag() || activeSqr.hasQmark() ) return;

                shaded = true;
                paintArea();

                if( activeSqr.hasMine() ) {
                    if( firstPress ) // give user a break on the first press
                    {
                        playSound( MineSwinger.FLAG );
                        activeSqr.setFlag( true );
                        setMinesText( --hiddenMines );
                    } else // game over
                    {
                        destroyed = true;
                        return;
                    }
                } else if( activeSqr.isBlank() ) {
                    firstScan = true;
                    clearOut( x, y );
                    cleared = true;
                } else
                    activeSqr.setRevealed();

                if( density == 0 ) {
                    paintAll = true;
                    repaint();
                }

                setScore( PLUS );

            }// BUTTON 1
            else
            // BUTTON 2 CLICKED
            if( (pressed & InputEvent.BUTTON2_DOWN_MASK) == InputEvent.BUTTON2_DOWN_MASK ) {
                if( activeSqr.hasFlag() ) return;

                if( activeSqr.hasQmark() ) {
                    // remove question mark
                    activeSqr.setQmark( false );
                } else { // add qmark if they are active
                    if( gameview.qMarksAreOn() ) activeSqr.setQmark( true );
                }
            }// BUTTON 2
            else
            // BUTTON 3 CLICKED
            if( (pressed & InputEvent.BUTTON3_DOWN_MASK) == InputEvent.BUTTON3_DOWN_MASK ) {
                if( activeSqr.hasFlag() ) // remove flag
                {
                    activeSqr.setFlag( false );
                    hiddenMines++;
                    setScore( MINUS );
                } else // add flag
                {
                    activeSqr.setQmark( false );
                    playSound( MineSwinger.FLAG );
                    activeSqr.setFlag( true );
                    hiddenMines--;
                    setScore( PLUS );
                }
            }// BUTTON 3
        } // Square NOT Revealed
    }

    /**
     * Call to repaint the area depending on changes from the preceding mouse press
     * @param evt - {@link MouseEvent}
     */
    @Override
    public void mouseReleased(final MouseEvent evt) {
        cleared = lightup = shaded = false;
        if( DEBUG_LEVEL > 1 )
            System.out.println( myname() + Msgs.str( "mouse.release" ) + InputEvent.getModifiersExText( evt.getModifiersEx() ) );

        if( firstPress ) firstPress = false;

        if( destroyed || badClear ) {
            destx = paintLeft;
            desty = paintTop;
            if( DEBUG_LEVEL > 2 )
                System.out.println( myname() + Msgs.str( "mouse.release" ) + "destx = " + destx + " ; " + "desty = " + desty );

            if( destroyed ) // activate the explosion
            {
                listener.init();
                setGameText( Msgs.str( "explode.info" ) );
                playSound( MineSwinger.BOOM );
                exploder.start();
            }

            paintAll = true;
            repaint();
            gameview.halt();
        } else
            paintArea();
    }

    /** Need to refresh only a certain portion of the frame - calls {@link java.awt.Component#repaint} */
    protected void paintArea() {
        if( DEBUG_LEVEL > 1 )
            System.out.println( myname() + ": paintArea() > " + Msgs.str( "Left" ) + paintLeft + Msgs.str( "Right" )
                            + paintRight + Msgs.str( "Top" ) + paintTop + Msgs.str( "Bottom" ) + paintBottom );
        //@formatter:off
        paintAll = false;
        repaint( paintLeft * squareLength,
                 paintTop * squareLength, 
                 (paintRight - paintLeft + 1) * squareLength, 
                 (paintBottom - paintTop + 1) * squareLength );
        //@formatter:on
    }

    /** Refresh the graphical representation of the mine array - called by {@link java.awt.Component#repaint} */
    @Override
    public void paintComponent(Graphics page) {
        if( DEBUG_LEVEL > 1 )
            System.out.println( myname() + ": paintComponent() > " + Msgs.str( "Left" ) + paintLeft + Msgs.str( "Right" )
                            + paintRight + Msgs.str( "Top" ) + paintTop + Msgs.str( "Bottom" ) + paintBottom );

        if( paintAll ) {
            paintLeft = 0;
            paintRight = fieldLength - 1;
            paintTop = 0;
            paintBottom = fieldLength - 1;
        }

        for( int i = paintLeft; i <= paintRight; i++ )
            for( int j = paintTop; j <= paintBottom; j++ ) {
                if( mine2dArray[i][j].isRevealed() )
                    reveal( i * squareLength, j * squareLength, mine2dArray[i][j].getMinecount(), page );
                else if( mine2dArray[i][j].hasFlag() )
                    reveal( i * squareLength, j * squareLength, FLAG, page );
                else if( mine2dArray[i][j].hasQmark() )
                    reveal( i * squareLength, j * squareLength, QMARK, page );
                else
                    reveal( i * squareLength, j * squareLength, BLANK, page );

                if( destroyed || badClear ) /* show all the mines */
                if( mine2dArray[i][j].hasMine() ) reveal( i * squareLength, j * squareLength, MINE, page );
            }

        if( badClear ) // indicate bad flag
            reveal( destx * squareLength, desty * squareLength, BADCLEAR, page );

        if( destroyed ) // blow up
            reveal( destx * squareLength, desty * squareLength, FATAL, page );

        paintAll = true;
    }

    /**
     * Draw individual <code>Squares</code> depending on the content. i.e. different numbers of adjacent mines, revealed or not,
     * flag, blow-up, etc<br>
     * - called ONLY by {@link #paintComponent(Graphics)}
     * @param xc - x co-ordinate
     * @param yc - y co-ordinate
     * @param tp - Square type
     * @param page - {@link Graphics} page
     */
    void reveal(final int xc, final int yc, final int tp, Graphics page) {
        int $type = tp;
        int $sl = squareLength;

        if( DEBUG_LEVEL > 2 ) System.out.println( myname() + ": reveal() > xc = " + xc + " & yc = " + yc + " & type = " + tp );

        // background
        if( lightup )
            page.setColor( COLOR_BLAZE );
        else if( shaded ) {
            page.setColor( COLOR_SHADE );
            $type = 0;
        } else
            page.setColor( COLOR_LIGHT );

        page.fillRect( xc, yc, squareLength, squareLength );

        // Square outline
        page.setColor( COLOR_MEDIUM );
        page.drawLine( xc, yc + $sl - 1, xc + $sl, yc + $sl - 1 ); // bottom
        page.drawLine( xc + $sl - 1, yc, xc + $sl - 1, yc + $sl ); // right
        page.setColor( COLOR_DARK );
        page.drawLine( xc, yc, xc + $sl, yc ); // top
        page.drawLine( xc, yc, xc, yc + $sl ); // left

        // interior details depending on type
        switch( $type ) {
        // 1 - 8 = # of adjacent mines: set color
        case 1:
            page.setColor( COLOR_NUM1 );
            break;
        case 2:
            page.setColor( COLOR_NUM2 );
            break;
        case 3:
            page.setColor( COLOR_NUM3 );
            break;
        case 4:
            page.setColor( COLOR_NUM4 );
            break;
        case 5:
            page.setColor( COLOR_NUM5 );
            break;
        case 6:
            page.setColor( COLOR_NUM6 );
            break;
        case 7:
            page.setColor( COLOR_NUM7 );
            break;
        case 8:
            page.setColor( COLOR_NUM8 );
            break;

        case MINE:
            drawSquare( xc, yc, page );
            page.setColor( COLOR_DARK );
            page.fillOval( xc + $sl / 8, yc + $sl / 8, 3 * $sl / 4, 3 * $sl / 4 );
            page.setColor( COLOR_DARKEST );
            page.fillOval( xc + $sl / 8 + 1, yc + $sl / 8 + 1, 3 * $sl / 4 - 2, 3 * $sl / 4 - 2 );
            page.setColor( COLOR_DARK );
            page.fillOval( xc + $sl / 3 - 2, yc + $sl / 3 - 2, $sl / 8 + 4, $sl / 8 + 4 );
            page.setColor( COLOR_MEDIUM );
            page.fillOval( xc + $sl / 3 - 1, yc + $sl / 3 - 1, $sl / 8 + 2, $sl / 8 + 2 );
            page.setColor( COLOR_BRIGHT );
            page.fillOval( xc + $sl / 3, yc + $sl / 3, $sl / 8, $sl / 8 );
            break;

        case FLAG:
            drawSquare( xc, yc, page );
            page.setColor( COLOR_FLAG_BKGRND );
            page.fillOval( xc + $sl / 8, yc + $sl / 8, 3 * $sl / 4, 3 * $sl / 4 );
            page.setColor( COLOR_FLAG_BELT );
            page.fillRect( xc + $sl / 8, yc + 3 * $sl / 8, 3 * $sl / 4, $sl / 4 );
            page.setColor( COLOR_FLAG_BURST );
            page.drawString( "*", xc + 3 * $sl / 8, yc + 7 * $sl / 8 );
            break;

        case QMARK:
            drawSquare( xc, yc, page );
            page.setColor( COLOR_DARKEST );
            page.drawString( "?", xc + $sl / 4, yc + 3 * $sl / 4 );
            break;

        case BLANK:
            drawSquare( xc, yc, page );
            break;

        case BADCLEAR:
            drawSquare( xc, yc, page );
            page.setColor( COLOR_DARK );
            page.fillOval( xc + $sl / 8, yc + $sl / 8, 3 * $sl / 4, 3 * $sl / 4 );
            page.setColor( COLOR_DARKEST );
            page.fillOval( xc + $sl / 8 + 1, yc + $sl / 8 + 1, 3 * $sl / 4 - 2, 3 * $sl / 4 - 2 );
            page.setColor( COLOR_DARK );
            page.fillOval( xc + $sl / 3 - 2, yc + $sl / 3 - 2, $sl / 8 + 4, $sl / 8 + 4 );
            page.setColor( COLOR_MEDIUM );
            page.fillOval( xc + $sl / 3 - 1, yc + $sl / 3 - 1, $sl / 8 + 2, $sl / 8 + 2 );
            page.setColor( COLOR_BRIGHT );
            page.fillOval( xc + $sl / 3, yc + $sl / 3, $sl / 8, $sl / 8 );
            page.setColor( COLOR_BOLD );
            // draw an 'X' over the erroneous mine placement
            page.drawString( "X", xc + $sl / 4, yc + 3 * $sl / 4 );
            break;

        case FATAL: // animated explosion if user clicked on a mine
        {
            if( DEBUG_LEVEL > 1 ) System.out.println( "case FATAL" );

            int index = listener.index % EXPLODE_INDEX;

            switch( index ) {
            case 0:
                page.setColor( COLOR_BRIGHT );
                page.fillOval( xc + $sl / 8, yc + $sl / 8, $sl - $sl / 4, $sl - $sl / 4 );
                page.fillOval( xc - $sl / 3, yc - $sl / 3, $sl / 5, $sl / 3 );
                page.fillOval( xc + 4 * $sl / 3, yc + $sl / 3, $sl / 5, $sl / 3 );
                break;

            case 1:
                page.setColor( COLOR_BLAZE );
                page.fillOval( xc + $sl / 8, yc + $sl / 8, $sl - $sl / 4, $sl - $sl / 4 );
                page.fillOval( xc - $sl / 2, yc - 2 * $sl / 5, $sl / 10, $sl / 10 );
                page.fillOval( xc + 2 * $sl, yc + $sl / 3, $sl / 5, $sl / 3 );

                page.setColor( COLOR_MEDIUM );
                page.fillOval( xc - $sl / 3, yc - $sl / 3, $sl / 5, $sl / 3 );
                page.fillOval( xc + 4 * $sl / 3, yc + $sl / 3, $sl / 5, $sl / 3 );
                break;

            case 2:
                page.setColor( COLOR_BOLD );
                page.fillOval( xc + $sl / 8, yc + $sl / 8, $sl - $sl / 4, $sl - $sl / 4 );
                page.fillOval( xc - 2 * $sl / 3, yc - 3 * $sl / 5, $sl / 10, $sl / 10 );
                page.fillOval( xc + 9 * $sl / 4, yc + $sl / 3, $sl / 5, $sl / 3 );

                page.setColor( COLOR_BLAZE );
                page.fillOval( xc + $sl, yc, $sl / 5, $sl / 5 );
                page.fillOval( xc, yc + $sl, $sl / 5, $sl / 5 );
                page.fillOval( xc + $sl, yc + $sl, $sl / 5, $sl / 5 );

                page.setColor( COLOR_MEDIUM );
                page.fillOval( xc - $sl / 2, yc - 2 * $sl / 5, $sl / 10, $sl / 10 );
                page.fillOval( xc + 2 * $sl, yc + $sl / 3, $sl / 5, $sl / 3 );
                break;

            case 3:
                page.setColor( COLOR_DARKEST );
                page.fillOval( xc + $sl / 8, yc + $sl / 8, $sl - $sl / 4, $sl - $sl / 4 );

                page.setColor( COLOR_MEDIUM );
                page.fillOval( xc - 2 * $sl / 3, yc - 3 * $sl / 5, $sl / 10, $sl / 10 );
                page.fillOval( xc + 9 * $sl / 4, yc + $sl / 3, $sl / 5, $sl / 3 );

                page.setColor( COLOR_BRIGHT );
                drawSparks( xc, yc, page );

                page.fillOval( xc + 5 * $sl / 4, yc - $sl / 4, $sl / 5, $sl / 5 );
                page.fillOval( xc - $sl / 4, yc + 5 * $sl / 4, $sl / 5, $sl / 5 );
                page.fillOval( xc + 5 * $sl / 4, yc + 5 * $sl / 4, $sl / 5, $sl / 5 );

                page.setColor( COLOR_MEDIUM );
                page.fillOval( xc + $sl, yc, $sl / 5, $sl / 5 );
                page.fillOval( xc, yc + $sl, $sl / 5, $sl / 5 );
                page.fillOval( xc + $sl, yc + $sl, $sl / 5, $sl / 5 );
                break;

            case 4:
                page.setColor( COLOR_BLAZE );
                drawSparks( xc, yc, page );

                page.fillOval( xc + 6 * $sl / 4, yc - 2 * $sl / 4, $sl / 7, $sl / 7 );
                page.fillOval( xc - 2 * $sl / 4, yc + 6 * $sl / 4, $sl / 7, $sl / 7 );
                page.fillOval( xc + 6 * $sl / 4, yc + 6 * $sl / 4, $sl / 7, $sl / 7 );

                page.setColor( COLOR_MEDIUM );
                page.fillOval( xc + 5 * $sl / 4, yc - $sl / 4, $sl / 5, $sl / 5 );
                page.fillOval( xc - $sl / 4, yc + 5 * $sl / 4, $sl / 5, $sl / 5 );
                page.fillOval( xc + 5 * $sl / 4, yc + 5 * $sl / 4, $sl / 5, $sl / 5 );
                break;

            case 5:
                page.setColor( COLOR_BLAST );
                drawSparks( xc, yc, page );

                page.fillOval( xc + 7 * $sl / 4, yc - 3 * $sl / 4, $sl / 10, $sl / 10 );
                page.fillOval( xc - 3 * $sl / 4, yc + 7 * $sl / 4, $sl / 10, $sl / 10 );
                page.fillOval( xc + 7 * $sl / 4, yc + 7 * $sl / 4, $sl / 10, $sl / 10 );

                page.setColor( COLOR_MEDIUM );
                page.fillOval( xc + 6 * $sl / 4, yc - 2 * $sl / 4, $sl / 7, $sl / 7 );
                page.fillOval( xc - 2 * $sl / 4, yc + 6 * $sl / 4, $sl / 7, $sl / 7 );
                page.fillOval( xc + 6 * $sl / 4, yc + 6 * $sl / 4, $sl / 7, $sl / 7 );
                break;

            case 6:
                page.setColor( COLOR_BOLD );
                drawSparks( xc, yc, page );

                page.setColor( COLOR_MEDIUM );
                page.fillOval( xc + 7 * $sl / 4, yc - 3 * $sl / 4, $sl / 10, $sl / 10 );
                page.fillOval( xc - 3 * $sl / 4, yc + 7 * $sl / 4, $sl / 10, $sl / 10 );
                page.fillOval( xc + 7 * $sl / 4, yc + 7 * $sl / 4, $sl / 10, $sl / 10 );
                break;

            case 7:
                page.setColor( COLOR_BRUN );
                drawSparks( xc, yc, page );
                break;

            default: // should always be == 8
                page.setColor( COLOR_BOLD.darker() );
                drawSparks( xc, yc, page );

                // turn off the animation
                exploder.stop();

            }// switch( index )

        }// end FATAL

        }// switch( type )

        // 1 - 8 = # of adjacent mines: draw numeral
        if( $type < MINE && $type > 0 ) {
            String $num = String.valueOf( $type );
            page.drawString( $num, xc + $sl / 4, yc + 3 * $sl / 4 );
        }
    }

    /**
     * Draw a basic unrevealed <code>Square</code> with edge hilites
     * @param xc - the <i>x</i> coordinate of the upper left corner
     * @param yc - the <i>y</i> coordinate of the upper left corner
     * @param page - Graphics object reference
     */
    protected void drawSquare(final int xc, final int yc, Graphics page) {
        int $sl = squareLength;

        page.setColor( COLOR_MEDIUM );
        page.fillRect( xc, yc, squareLength, squareLength );

        page.setColor( COLOR_DARKEST );
        page.drawLine( xc, yc + $sl - 1, xc + $sl, yc + $sl - 1 ); // bottom
        page.drawLine( xc + $sl - 1, yc, xc + $sl - 1, yc + $sl ); // right

        page.setColor( COLOR_LIGHT );
        page.drawLine( xc, yc, xc + $sl - 1, yc ); // top
        page.drawLine( xc, yc, xc, yc + $sl - 1 ); // left
    }

    /**
     * Part of the animation explosion sequence
     * @param xc - <i>x</i> co-ordinate of the upper left corner
     * @param yc - <i>y</i> co-ordinate of the upper left corner
     * @param page - Graphics object reference
     * @see java.awt.Graphics#fillOval(int, int, int, int)
     */
    protected void drawSparks(final int xc, final int yc, Graphics page) {
        int $sl = squareLength;

        page.fillOval( xc + $sl / 2, yc + $sl / 8, $sl / 10, $sl / 10 );
        page.fillOval( xc + 7 * $sl / 10, yc + $sl / 2, $sl / 10, $sl / 10 );
        page.fillOval( xc + 2 * $sl / 10, yc + 7 * $sl / 10, $sl / 10, $sl / 10 );
        page.fillOval( xc + $sl / 3, yc + 3 * $sl / 5, $sl / 10, $sl / 10 );
        page.fillOval( xc + $sl / 4, yc + $sl / 2, $sl / 10, $sl / 10 );
        page.fillOval( xc + $sl / 8, yc + $sl / 8, $sl / 10, $sl / 10 );
        page.fillOval( xc + $sl / 4, yc + 2 * $sl / 3, $sl / 10, $sl / 10 );
        page.fillOval( xc + 7 * $sl / 10, yc + 2 * $sl / 5, $sl / 10, $sl / 10 );
        page.fillOval( xc + $sl / 2, yc + $sl / 5, $sl / 10, $sl / 10 );
        page.fillOval( xc + 3 * $sl / 4, yc + $sl / 2, $sl / 10, $sl / 10 );
        page.fillOval( xc + $sl / 3, yc + $sl / 3, $sl / 3, $sl / 3 );
        page.fillOval( xc + 3 * $sl / 5, yc + $sl / 3, $sl / 10, $sl / 10 );
        page.fillOval( xc + 2 * $sl / 5, yc + 2 * $sl / 5, $sl / 10, $sl / 10 );
        page.fillOval( xc + $sl / 2, yc + $sl / 5, $sl / 10, $sl / 10 );
        page.fillOval( xc + 2 * $sl / 5, yc + 1 * $sl / 5, $sl / 10, $sl / 10 );
        page.fillOval( xc + 3 * $sl / 5, yc + 4 * $sl / 5, $sl / 10, $sl / 10 );
    }

    /*
     *     INNER CLASSES
     * ========================================================================================= */

    /**
     * The data structure behind every square in the minefield. - class <code>MineField</code> uses a 2D array of
     * <code>Squares</code>
     */
    class Square {
        private boolean hasMine = false, isRevealed = false, hasFlag = false, hasQmark = false;

        /** The total number of mines in all neighbouring <code>Squares</code> */
        private int mineCount;

        //@formatter:off
        /**
         * Set the minecount variable of an individual <code>Square</code>
         * @param count int
         */
        void setMinecount(final int count) { mineCount = count;}

        /** @return The total number of mines in all neighbouring <code>Squares</code> */
        int getMinecount() { return mineCount;}

        /** @return true if NO adjacent mines, otherwise false */
        boolean isBlank() { return mineCount == 0;}

        /** @return true if this <code>Square</code> has a mine, otherwise false */
        boolean hasMine() { return hasMine;}

        /** Plants a mine in this <code>Square</code> */
        void arm() { hasMine = true;}

        /** @return whether a <code>Square</code> has been revealed or not */
        boolean isRevealed() { return isRevealed;}

        /** Set value of <var>isRevealed</var> to TRUE */
        void setRevealed() { isRevealed = true;}

        /** @return whether a <code>Square</code> has a flag placed (indicating a mine underneath) */
        boolean hasFlag() { return hasFlag;}

        /**
         * Set value of <var>hasFlag</var> to parameter value
         * @param flag - boolean
         */
        void setFlag(final boolean flag) { hasFlag = flag;}

        /** @return whether a <code>Square</code> has a question mark (indicating doubt about a mine placement) */
        boolean hasQmark() { return hasQmark;}

        /**
         * Set value of hasQmark to parameter value
         * @param qmark boolean
         */
        void setQmark(final boolean qmark) { hasQmark = qmark;}
        //@formatter:on
    }/* inner class Square */

    /**
     * Listens for the <code>Timer</code> events which drive the animation explosion
     * @see MineField#exploder
     * @see ActionListener
     */
    class ExplodeListener implements ActionListener {
        private int index;

        /** @return simple name of my Class */
        String myname() {
            return this.getClass().getSimpleName();
        }

        private void init() {
            index = 0;
        }

        @Override
        public void actionPerformed(final ActionEvent ae) {
            if( ae.getSource() == exploder ) {
                setMinesText( ++index );
                if( DEBUG_LEVEL > 0 ) System.out.println( myname() + ": Event index = " + index );

                repaint();
            } else
                System.err.println( myname() + ": Event source INVALID!" );
        }

    }/* inner class ExplodeListener */

}/* class MineField */
