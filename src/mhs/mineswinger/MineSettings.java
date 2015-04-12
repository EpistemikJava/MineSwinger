/* ******************************************************************************
 * 
 *  Mark Sattolo (epistemik@gmail.com)
 * -----------------------------------------------
 *  $File: //depot/Eclipse/Java/workspace/MineSwinger/src/mhs/mineswinger/MineSettings.java $
 *  $Revision: #8 $ 
 *  $Change: 155 $ 
 *  $DateTime: 2011/12/25 00:33:16 $
 * -----------------------------------------------
 * 
 * MineSettings.java
 * Created on Jan 29, 2008, 19h23
 * Initial git version created Apr 12, 2015
 * 
 ********************************************************************************/

package mhs.mineswinger;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * MineSettings is a sub-Window used to adjust the mine density and/or square size.
 * 
 * @author  MARK SATTOLO (based on code by Mats Antell)
 * @version $Revision: #8 $
 */
class MineSettings extends JFrame implements ActionListener
{
 /*
  *     FIELDS
  * ========================================================================================= */
  
  /** Keep the compiler from complaining...  */
  private static final long serialVersionUID = -9135300519496881599L;
  
  static final int
                  WINDOW_DEFAULT_WIDTH  = 520 ,
                  WINDOW_DEFAULT_HEIGHT = 260 ,
                   
                  DENSITY_MAJOR_TICK_SPACING = 20 ,
                  DENSITY_MINOR_TICK_SPACING =  5 ;
  
  private int DEBUG_LEVEL ;
  
  private double density ;
  private int percentDensity, squareLength ;
  
  /** reference to the enclosing class */
  private MineSwinger gameview ;
  
  /** identify the source of action events */
  private Object source ;
  
  private JPanel denPanel, sizePanel, btnPanel ;
  private JLabel denHead, denInfo, sizeHead ;
  
  private JSlider denSlider ;
  private JButton newBtn, sizeBtn, cancelBtn ;
  
  private ButtonGroup sizeGroup ;
  private JRadioButton smallRadBtn, medRadBtn, largeRadBtn ;
  
  private boolean startNew = false ;

 /*
  *     METHODS
  * ========================================================================================= */

  /**
   * CONSTRUCTOR <br> called ONLY by <code>MineSwinger.startup()</code>
   * @param game - reference to the enclosing {@link MineSwinger}
   * @see #buildComponents()
   */
  MineSettings( final MineSwinger game )
  {
    DEBUG_LEVEL = game.DEBUG_LEVEL ;
    if( DEBUG_LEVEL > 0 )
      System.out.println( myname() + Msgs.str("debug_level") + DEBUG_LEVEL ); //$NON-NLS-1$
    
    gameview = game ;
    squareLength = MineSwinger.SQUARE_SIZE_MD ;
    
    setFont( MineSwinger.fontMEDIUM );
    setTitle( Msgs.str("settings.change") ); //$NON-NLS-1$
    setSize( WINDOW_DEFAULT_WIDTH, WINDOW_DEFAULT_HEIGHT );
    
    // the window listener will handle closing
    setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
    
    addWindowListener(
      new WindowAdapter()
        { @Override
          public void windowClosing( WindowEvent we )
          {
            gameview.setMineSettingsOpen( false );
            setVisible( false );
          }
        }
      );
    
    getContentPane().setLayout( new GridLayout(3,1,3,3) );
    
    buildComponents();
    
    game.setMineSettingsOpen( true );
    
  }//! Constructor
  
  /**
   * Build the panels comprising this JFrame 
   * @see #buildDensityPanel()
   * @see #buildSizePanel()
   * @see #buildButtonPanel()
   */
  private void buildComponents()
  {
    buildDensityPanel();
    buildSizePanel();
    buildButtonPanel();
    
    // insert the panels
    getContentPane().add( denPanel  );
    getContentPane().add( sizePanel );
    getContentPane().add( btnPanel  );
    
  }// buildComponents()
  
  /**
   * Contains the density slider 
   * @see #setDensity(int)
   */
  private void buildDensityPanel()
  {
    // density slider
    denPanel = new JPanel();
    density = gameview.mineField.getDensity();
    percentDensity = (int)( density * 100  );
    denHead = new JLabel( Msgs.str("density.mine") ); //$NON-NLS-1$
    denInfo = new JLabel( Integer.toString(percentDensity) + " %" ); //$NON-NLS-1$
    
    denSlider = new JSlider( 0, 100, percentDensity );
    denSlider.setMajorTickSpacing( DENSITY_MAJOR_TICK_SPACING );
    denSlider.setMinorTickSpacing( DENSITY_MINOR_TICK_SPACING );
    denSlider.setPaintTicks( true );
    denSlider.setPaintLabels( true );
    
    denSlider.addChangeListener(
      new ChangeListener()
        {
          @Override
          public void stateChanged( final ChangeEvent ce )
          {
            setDensity( ((JSlider)ce.getSource()).getValue() );
          }
        }
      );
    
    denPanel.add( denHead   );
    denPanel.add( denInfo   );
    denPanel.add( denSlider );
   
  }// buildDensityPanel()
  
  /** Change the size of MineField Squares  */
  private void buildSizePanel()
  {
    // radio buttons for Square size
    sizePanel = new JPanel();
    sizeHead  = new JLabel( Msgs.str("size.square") ); //$NON-NLS-1$
    sizeGroup = new ButtonGroup();
    
    smallRadBtn = new JRadioButton( Msgs.str("small"), false ); //$NON-NLS-1$
    smallRadBtn.addActionListener( this );
    medRadBtn   = new JRadioButton( Msgs.str("Medium"), true ); //$NON-NLS-1$
    medRadBtn.addActionListener( this );
    largeRadBtn = new JRadioButton( Msgs.str("LARGE"), false ); //$NON-NLS-1$
    largeRadBtn.addActionListener( this );
    
    sizeGroup.add( smallRadBtn );
    sizeGroup.add( medRadBtn );
    sizeGroup.add( largeRadBtn );
    
    sizePanel.add( sizeHead   );
    sizePanel.add( smallRadBtn );
    sizePanel.add( medRadBtn );
    sizePanel.add( largeRadBtn );
    
  }// buildSizePanel()
  
  /** Contains the buttons for new game, change size and cancel  */
  private void buildButtonPanel()
  {
    // action buttons
    btnPanel  = new JPanel();
    newBtn    = new JButton( Msgs.str("game.new") ); //$NON-NLS-1$
    sizeBtn   = new JButton( Msgs.str("size.new") ); //$NON-NLS-1$
    cancelBtn = new JButton( Msgs.str("Cancel")   ); //$NON-NLS-1$
    
       newBtn.addActionListener( this );
      sizeBtn.addActionListener( this );
    cancelBtn.addActionListener( this );
    
    btnPanel.add( newBtn    );
    btnPanel.add( sizeBtn   );
    btnPanel.add( cancelBtn );

  }// buildButtonPanel()
  
  /** @return simple name of my Class  */
  String myname() { return this.getClass().getSimpleName(); }
  
  /**
   * Respond to events
   * @see #confirm()
   */
  @Override
  public void actionPerformed( final ActionEvent ae )
  {
    if( DEBUG_LEVEL > 0 )
      System.out.println( myname() + ": Action command = " + ae.getActionCommand() ); //$NON-NLS-1$
    
    source = ae.getSource();
    
    if( source == cancelBtn )
    {
      gameview.setMineSettingsOpen( false );
      dispose();
    }
    else if( source == smallRadBtn )
      squareLength = MineSwinger.SQUARE_SIZE_SM ;
    else if( source == medRadBtn )
      squareLength = MineSwinger.SQUARE_SIZE_MD ;
    else if( source == largeRadBtn )
      squareLength = MineSwinger.SQUARE_SIZE_LG ;
    else
    {
      if( source == newBtn )
        startNew = true ;
      else if( source == sizeBtn )
        startNew = false ;

        confirm();
        dispose();
    }
    
  }// actionPerformed()
  
  /** Called by the density slider 
   * @param percent - specified by the slider  */
  void setDensity( final int percent )
  {
    density = percent/100.0 ;
    percentDensity = percent ;
    denInfo.setText( Integer.toString(percentDensity) + " %" ); //$NON-NLS-1$
    
  }// setDensity()
  
  /** Accept new values and close */
  protected void confirm()
  {
    gameview.newSquareLength( squareLength );
    
    if( startNew )
    {
      gameview.adjustDensity( density );
      gameview.halt();
      gameview.newGame();
    }
    
    gameview.setMineSettingsOpen( false );
    
  }// confirm()
  
}/* Class MineSettings */
