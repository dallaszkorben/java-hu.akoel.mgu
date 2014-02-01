package hu.akoel.mgu.drawnblock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.border.Border;
import javax.swing.event.MouseInputListener;

import hu.akoel.mgu.MCanvas;
import hu.akoel.mgu.MGraphics;
import hu.akoel.mgu.PainterListener;
import hu.akoel.mgu.PossiblePixelPerUnits;
import hu.akoel.mgu.drawnblock.DrawnBlock.Status;
import hu.akoel.mgu.values.PositionValue;
import hu.akoel.mgu.values.SizeValue;
import hu.akoel.mgu.values.TranslateValue;

public class DrawnBlockCanvas extends MCanvas{

	private static final long serialVersionUID = -8308688255617119442L;
	
	private Stroke basicStroke = new BasicStroke();
	
	private DrawnBlockFactory drawnBlockFactory;
	
	private ArrayList<DrawnBlock> drawnBlockList = new ArrayList<DrawnBlock>();
	private ArrayList<DrawnBlock> temporaryDrawnBlockList = new ArrayList<DrawnBlock>();
	
	private DrawnBlockDrawListener drawnBlockDrawListener = new DrawnBlockDrawListener();
	private DrawnBlockPainterListener drawnBlockPainterListener = new DrawnBlockPainterListener();
	
	private SecondaryCursor secondaryCursor = new SecondaryCursor();
	
	private boolean needFocus = true;
	
	private boolean neededSnapSideExtention = true;
	private boolean neededSnapGrid = true;

	/**
	 * Engedelyezi a masodlagos kurzor legkozelebbi DrawnBlock oldalhoz, vagy annak
	 * meghosszabbitasahoz valo igazitasat
	 * 
	 * @param needed
	 */
	public void setNeededSnapSideExtention( boolean needed ){
		this.neededSnapSideExtention = needed;
	}
	
	public boolean getNeededSnapSideExtention(){
		return neededSnapSideExtention;
	}

	/**
	 * Engedelyezi a masodlagos kurzor legkozelebbi Grid ponthoz valo igazitasat
	 * 
	 * @param needed
	 */
	public void setNeededSnapGrid( boolean needed ){
		this.neededSnapGrid = needed;
	}
	
	public boolean getNeededSnapGrid(){
		return neededSnapGrid;
	}
	
	public DrawnBlockCanvas(Border borderType, Color background, PossiblePixelPerUnits possiblePixelPerUnits, TranslateValue positionToMiddle ) {
		super(borderType, background, possiblePixelPerUnits, positionToMiddle );		
		commonConstructor();
	}
	
	public DrawnBlockCanvas(Border borderType, Color background, PossiblePixelPerUnits possiblePixelPerUnits, TranslateValue positionToMiddle, SizeValue boundSize ) {
		super(borderType, background, possiblePixelPerUnits, positionToMiddle, boundSize );
		commonConstructor();
	}

	private void commonConstructor(){
		
		//Azt figyeli, hogy egy DrawnBlock fokuszba kerult-e
//		this.addMouseMotionListener( drawnBlockInFocusListener );
		
		//A kozepso reteget hasznaljuk a DrawnBlock-ok megjelenitesere
		this.addPainterListenerToMiddle( drawnBlockPainterListener, Level.ABOVE );
		
		//A kurzor mozgasat vizsgalolo listener
		this.addMouseInputListener( drawnBlockDrawListener );
		
//		//Egy DrawnBlock mozgatasat figyeli
//		this.addMouseInputListener( DragListener );
		
	}

	public void setNeedFocus( boolean needFocus){
		this.needFocus = needFocus;
	}
	
	public boolean needFocus(){
		return needFocus;
	}
	
	public void setDrawnBlockFactory( DrawnBlockFactory drawnBlockFactory ){
		this.drawnBlockFactory = drawnBlockFactory;
	}
	
	/**
	 * Hozzaad a megjelenitendo listahoz egy DrawnBlock-ot
	 * 
	 * @param drawnBlock
	 */
	public void addDrawnBlock( DrawnBlock drawnBlock ){

		if( !this.drawnBlockList.contains( drawnBlock ) ){
			this.drawnBlockList.add( drawnBlock );
		}		
	}
	
	/**
	 * Eltavolit egy DrawnBlock elemet a megjelenitendo DrawnBlock listabol
	 * 
	 * @param drawnBlock
	 */
	public void removeDrawnBlock( DrawnBlock drawnBlock ){
		this.drawnBlockList.remove( drawnBlock );
	}
		
	/**
	 * Hozzaad egy DrawnBlock elemet a Temporary listahoz atmeneti megjelenitesre
	 * 
	 * @param drawnBlock
	 */
	public void addTemporaryDrawnBlock( DrawnBlock drawnBlock ){
		
		//Temporary reteget hasznaljuk a fokus megjelenitesre
		//Minden megjelenites utan torlodik a listener, ezert kell mindig hozzaadni
		this.addPainterListenerToTemporary( new TemporaryDrawnBlockPainterListener(), Level.UNDER );	
		
		if( !temporaryDrawnBlockList.contains(drawnBlock)){
			temporaryDrawnBlockList.add(drawnBlock);
		}	
	}
	
	public void addTemporarySecondaryCursor( SecondaryCursor secondaryCursor ){
		
		//Minden megjelenites utan torlodik a listener, ezert kell mindig hozzaadni
		this.addPainterListenerToTemporary( new TemporarySecondaryCursorPainterListener(), Level.ABOVE );
		
		//Itt nincs szukseg a lista megadasara, mert csak egy elem szerepel
	}

	public void zoomIn(double xCenter, double yCenter, int xPoint, int yPoint){
		super.zoomIn(xCenter, yCenter, xPoint, yPoint);

		//Azert kell, hogy a zoom utan kovetkezo ujrarajzolas miatt eltuno fokuszban levo DrawnBlock ujra bekeruljon a temporary listaba
		//Mozgast szimulal, mintha megmozdult volna a kurzor, ami azt eredmenyezi, hogy kirajzolodik a kurzor
		//fireMouseMoved();

		drawnBlockDrawListener.repaintSecondaryCursorAndDrawnBlockToDraw();
	}
	
	public void zoomOut(double xCenter, double yCenter, int xPoint, int yPoint){
		super.zoomOut(xCenter, yCenter, xPoint, yPoint);
		
		//Azert kell, hogy a zoom utan kovetkezo ujrarajzolas miatt eltuno fokuszban levo DrawnBlock ujra bekeruljon a temporary listaba

//		fireMouseMoved();
		
		drawnBlockDrawListener.repaintSecondaryCursorAndDrawnBlockToDraw();

	}
	

	
	/**
	 * 
	 * DrawnBlock rajzolasaert felelos osztaly
	 * 
	 * move, exited
	 * exited, dragged
	 * pressed, released clicked
	 * 
	 * @author akoel
	 *
	 */
	class DrawnBlockDrawListener implements MouseInputListener{
		
		private PositionValue secondaryStartCursorPosition = new PositionValue(0, 0);
		
		private boolean drawnStarted = false;
		private DrawnBlock drawnBlockToDraw = null;


		@Override
		public void mouseClicked(MouseEvent e) {
			
//System.err.println("clicked");			
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			
			//Ha a baloldali egergombot nyomtam es meg nem kezdtem el rajzolni
			if( e.getButton() == MouseEvent.BUTTON1 && !drawnStarted && null != drawnBlockFactory ){
				
				//A kurzor pozicioja
				secondaryStartCursorPosition.setX( secondaryCursor.getX() );
				secondaryStartCursorPosition.setY( secondaryCursor.getY() );

				drawnStarted = true;
				
				//A szerkesztendo DrawnBlock legyartasa
				drawnBlockToDraw = drawnBlockFactory.getNewDrawnBlock( Status.INPROCESS, secondaryStartCursorPosition.getX(), secondaryStartCursorPosition.getY() ); 
						
				//Atmeneti listaba helyezi a most rajzolas alatt levo DrawnBlock-ot
				addTemporaryDrawnBlock( drawnBlockToDraw );

			}

//System.err.println("pressed");			
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			
			//Ha elindult mar egy rajzolasi folyamat
			if( drawnStarted ){
				
				//Jelzi, hogy meg nem indult el a kovetkezo DrawnBlock rajzolasa
				drawnStarted = false;
				
				if( drawnBlockToDraw.getX1() != drawnBlockToDraw.getX2() || drawnBlockToDraw.getY1() != drawnBlockToDraw.getY2()){
												
					//A lehelyezendo DrawnBlokk statusza NORMAL lesz
					drawnBlockToDraw.setStatus( Status.NORMAL );
				
					//Hozzaadom a statikusan kirajzolando DrawnBlock-ok listajahoz
					addDrawnBlock( drawnBlockToDraw );

				}
				
				//Az ujabb DrawnBlock meg nem letezik
				drawnBlockToDraw = null;
				
				//Azert kell, hogy az elengedes pillanataban ne tunjon el a masodlagos kurzor
				addTemporarySecondaryCursor(secondaryCursor);
				
				//Ujrarajzoltatom a Canvas-t az uj statikus DrawnBlock-kal egyutt
				revalidateAndRepaintCoreCanvas();
								
			}
			
//System.err.println("release");
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
//System.err.println("entered");	
			
			//Meghatarozza a masodlagos kurzor aktualis erteket
			findOutCursorPosition( e );

			//Kirajzolja a masodlagos kurzort
			addTemporarySecondaryCursor( secondaryCursor );
			
repaintCoreCanvas();
//revalidateAndRepaintCoreCanvas();

		}

		@Override
		public void mouseExited(MouseEvent e) {
//System.err.println("exited");			
			revalidateAndRepaintCoreCanvas();
			
		}

		@Override
		public void mouseDragged(MouseEvent e) {

//System.err.println("dragged");			
			
			//Meghatarozza a masodlagos kurzor aktualis erteket
			findOutCursorPosition( e );
			
			//Ha mar elkezdtem rajzolni
			if( drawnStarted ){
	
				// DrawnBlock meretet megvaltoztatja - automatikusan sorba rendezi a koordinatakat x1 <= x2, y1 <= y2
				drawnBlockToDraw.changeSize( secondaryCursor.getX(), secondaryCursor.getY() );
			
				//Elhelyezni a temporary listaban a most szerkesztendo DrawnBlock-ot
				addTemporaryDrawnBlock( drawnBlockToDraw );
			}

			//Elhelyezi a temporary listaban a masodlagos kurzort
			addTemporarySecondaryCursor( secondaryCursor );
			
			//Kirajzolja az elhelyezett szerkesztedno DrawnBlock-ot es a masodlagos kurzort
repaintCoreCanvas();
//revalidateAndRepaintCoreCanvas();
			
		}

		@Override
		public void mouseMoved(MouseEvent e) {
//System.err.println("moved");
			
			//Meghatarozza a masodlagos kurzor aktualis erteket
			findOutCursorPosition( e );
			
			//Kirajzolja a masodlagos kurzort
			addTemporarySecondaryCursor( secondaryCursor );
			
			//Kirajzolja a masodlagos kurzort
repaintCoreCanvas();
//revalidateAndRepaintCoreCanvas();

		}
		
		
		class Arrange{
			DrawnBlock drawnBlockToArrangeX = null;
			DrawnBlock drawnBlockToArrangeY = null;
			
			Double positionX = null;
			Double positionY = null;
			
			public void addDrawnBlockToArrangeX( DrawnBlock drawnBlockToArrange, Double position ){
				this.drawnBlockToArrangeX = drawnBlockToArrange;
				this.positionX = position;
			}

			public void addDrawnBlockToArrangeY( DrawnBlock drawnBlockToArrange, Double position ){
				this.drawnBlockToArrangeY = drawnBlockToArrange;
				this.positionY = position;
			}
			
			public Double getPositionX(){
				return positionX;
			}

			public Double getPositionY(){
				return positionY;
			}

		}
		
		/**
		 * Meghatarozza a masodlagos kurzor aktualis erteket
		 * 
		 * @param e
		 */
		private void findOutCursorPosition( MouseEvent e ){

			double tmpX1, tmpX2, tmpY1, tmpY2;
			
			double x = getWorldXByPixel( e.getX() );
			double y = getWorldYByPixel( e.getY() );
			
			//-------------------------------------------------------------------------------
			//
			// DrawnBlock-ok oldalvonalahoz probalja igazitani a masodlagos kurzor poziciojat
			//
			// -Vegig megy minden statikusan lehelyezett DrawnBlock-on
			// -Megnezi, hogy valamelyik oldalahoz, vagy az oldalanak meghosszabbitasahoz eleg
			//  kozel van-e a kurzor
			// -X es Y iranyba a Kurzorhoz a legkozelebb levo oldalhoz (ha az adott kozelsegen belul van)
			//  igazitja a Masodlagos Kurzort
			//
			//-------------------------------------------------------------------------------
			int delta = 15;
			double dx = getWorldXLengthByPixel( delta );
			double dy = getWorldXLengthByPixel( delta );
			
			double minDX = Double.MAX_VALUE;
			double minDY = Double.MAX_VALUE;
			Arrange arrange = new Arrange();
			
			for( DrawnBlock db : drawnBlockList ){
				
				//Ha megfelelo kozelsegben vagyok az egyik lehelyezett DrawnBlock-hoz. 
				//if( db.intersects( new Block( x-dx, y-dy, x+dx, y+dy) ) ){
					
					//Bal oldalrol kozeliti a DrawnBlock balo ldalat
					if( ( db.getX1() - x ) > 0 && ( db.getX1() - x ) < dx ){
						
						//Ha ez kozelebb van, mint az eddigi legkozelebbi
						if( ( db.getX1() - x ) < minDX ){
							minDX = db.getX1() - x;
							arrange.addDrawnBlockToArrangeX( db, db.getX1() );							
						}
						
					//!!! Bal oldalrol kozeliti a DrawnBlock jobboldalat !!!
					}else if( ( db.getX2() - x ) > 0 && ( db.getX2() - x ) < dx ){
							
						//Ha ez kozelebb van, mint az eddigi legkozelebbi
						if( ( db.getX2() - x ) < minDX ){
							minDX = db.getX2() - x;
							arrange.addDrawnBlockToArrangeX( db, db.getX2() );							
						}
						
					//Jobb oldalrol kozeliti a DrawnBlock jobb oldalat
					}else if( ( x - db.getX2() ) > 0 && ( x - db.getX2() ) < dx ){

						//Ha ez kozelebb van, mint az eddigi legkozelebbi
						if( ( x - db.getX2() ) < minDX ){
							minDX = x - db.getX2();
							arrange.addDrawnBlockToArrangeX( db, db.getX2() );							
						}
					
					//!!! Jobb oldalrol kozeliti a DrawnBlock bal oldalat !!!
					}else if( ( x - db.getX1() ) > 0 && ( x - db.getX1() ) < dx ){

						//Ha ez kozelebb van, mint az eddigi legkozelebbi
						if( ( x - db.getX1() ) < minDX ){
							minDX = x - db.getX1();
							arrange.addDrawnBlockToArrangeX( db, db.getX1() );							
						}						
					}
					
					//Fentrol kozeliti a DrawnBlock tetejet
					if( ( y - db.getY2() ) > 0 && ( y - db.getY2() ) < dy ){
						
						//Ha ez kozelebb van, mint az eddigi legkozelebbi
						if( ( y - db.getY2() ) < minDY ){
							minDY = y - db.getY2();
							arrange.addDrawnBlockToArrangeY( db, db.getY2() );							
						}
					
					//!!! Fentrol kozeliti a DrawBlock aljat !!!
					}else if( ( y - db.getY1() ) > 0 && ( y - db.getY1() ) < dy ){
						
						//Ha ez kozelebb van, mint az eddigi legkozelebbi
						if( ( y - db.getY1() ) < minDY ){
							minDY = y - db.getY1();
							arrange.addDrawnBlockToArrangeY( db, db.getY1() );							
						}						
					
					//Alulrol kozeliti a DrawnBlock aljat
					}else if( ( db.getY1() - y ) > 0 && ( db.getY1() - y ) < dy ){

						//Ha ez kozelebb van, mint az eddigi legkozelebbi
						if( ( db.getY1() - y ) < minDY ){
							minDY = db.getY1() - y;
							arrange.addDrawnBlockToArrangeY( db, db.getY1() );							
						}						
					
					//!!! Alulrol kozeliti a DrawnBlock tetejet !!!
					}else if( ( db.getY2() - y ) > 0 && ( db.getY2() - y ) < dy ){

						//Ha ez kozelebb van, mint az eddigi legkozelebbi
						if( ( db.getY2() - y ) < minDY ){
							minDY = db.getY2() - y;
							arrange.addDrawnBlockToArrangeY( db, db.getY2() );							
						}
					}	
				//}				
			}
			
			//
			// Most vegzi el a kurzor leendo uj koordinatainak modositasat. 
			// Meg nem tolti be a Masodlagos Kurzorba
			//
			//if( null != arrange ){
				if( null != arrange.getPositionX() ){
					x = arrange.getPositionX();
				}
				
				if( null != arrange.getPositionY() ){
					y = arrange.getPositionY();
				}
			//}
			
			
			
			
			//-----------------------------------
			//
			// Atfedesek elkerulesenek vezerlese
			//
			//-----------------------------------
			
			//
			// Ha meg nem kezdodott el a rajzolas, szabadon mozgo kurzor
			//
//TODO itt meg kellene oldani, hogy ha ket egymast erinto blokk koze kerulne, az nem OK				
			if( !drawnStarted ){

				//Megnezi, hogy az aktualis kurzor egy lehelyezett DrawnBlock-ra esik-e
				for( DrawnBlock db: drawnBlockList ){
				
					//Beleesik a kurzor egy lehelyezett DrawnBlock belsejeben
					if( ( x > db.getX1() && x < db.getX2() ) && ( y > db.getY1() && y < db.getY2() ) ){

						//Ha az elobbi X poziciot hasznalom, akkor kivul kerulok
						if( ( secondaryCursor.getX() <= db.getX1() || secondaryCursor.getX() >= db.getX2() ) && ( y > db.getY1() && y < db.getY2() ) ){
							x = secondaryCursor.getX();
						
						//Ha az elobbi Y poziciot hasznalom, akkor kivul kerulok
						}else if( ( x > db.getX1() && x < db.getX2() ) && ( secondaryCursor.getY() <= db.getY1() || secondaryCursor.getY() >= db.getY2() ) ){
							y = secondaryCursor.getY();
						
						//Kulonben
						}else{
						
							//Akkor a masodlagos kurzor marad a regi pozicioban
							return;
						}
					}
				}
			
			//
			// Ha mar elkezdte a rajzolast
			//
			}else{
				
				//
				// A feltetelezett uj DrawnBlock koordinatainak nagysag szerinti rendezese
				//
				if( x <= secondaryStartCursorPosition.getX() ){
					tmpX1 = x;
					tmpX2 = secondaryStartCursorPosition.getX();
				}else{					
					tmpX1 = secondaryStartCursorPosition.getX();
					tmpX2 = x;
				}
				
				if( y <= secondaryStartCursorPosition.getY() ){
					tmpY1 = y;
					tmpY2 = secondaryStartCursorPosition.getY();
				}else{					
					tmpY1 = secondaryStartCursorPosition.getY();
					tmpY2 = y;
				}
				
				// Vegig a lehelyezett DrawnBlock-okon
				for( DrawnBlock db: drawnBlockList ){
					
					//Ha a most szerkesztett DrawnBlock fedesbe kerulne egy mar lehelyezett DrawnBlock-kal
					if( db.intersectsOrContains( new Rectangle.Double( tmpX1, tmpY1, tmpX2-tmpX1, tmpY2-tmpY1 ) )){
						
						//Akkor marad a regi kurzorpozicio
						return;
					}
			
				}
				
			}
			
			
			//------------------------------------------------
			//
			// A szerkesztendo elem megengedi-e az uj poziciot
			//
			//-------------------------------------------------
			if( drawnStarted && null != drawnBlockToDraw && !drawnBlockToDraw.enabledToChange( x, y ) ){
				return;				
			}
			
			
			//A Masodlagos kurzor poziciojanak beallitasa
			secondaryCursor.setPosition( x, y );
		}
		
			
		/**
		 * Megjeleniti a kurzort es a szerkesztes alatt allo DrawnBlock-ot
		 * az atmeneti retegben
		 * 
		 */
		public void repaintSecondaryCursorAndDrawnBlockToDraw(){
			
			if( null != drawnBlockToDraw ){
				addTemporaryDrawnBlock( drawnBlockToDraw );
			}
			
			if( null != secondaryCursor ){
				addTemporarySecondaryCursor( secondaryCursor );
			}
			
			repaintCoreCanvas();
			
		}
		
	}
	

	
	
	/**
	 * Azt figyeli, hogy egy DrawnBlock fokuszba kerult-e
	 * 
	 * @author akoel
	 *
	 */
/*	class DrawnBlockInFocusListener implements MouseMotionListener{
		
		@Override
		public void mouseMoved(MouseEvent e) {
	
			if( needFocus() ){
			
				double xValue = getWorldXByPixel(e.getX() );			
				double yValue = getWorldYByPixel(e.getY());
				boolean needToPrint = false;

				for( DrawnBlock sprite: drawnBlockList){
				
					SizeValue boundBox = sprite.getBoundBoxAbsolute();						
			
					if( 
						xValue >= boundBox.getXMin() &&
						xValue <= boundBox.getXMax() &&
						yValue >= boundBox.getYMin() &&
						yValue <= boundBox.getYMax()
					){
											
						addTemporarySprite(sprite);						
						needToPrint = true;
						sprite.setInFocus(true);

					}else{
						if( sprite.isInFocus() ){
							needToPrint = true;						
							sprite.setInFocus(false);
						}
					}					
				}
				if( needToPrint ){
					repaintCoreCanvas();
				}
			}				
		}
			
		@Override
		public void mouseDragged(MouseEvent e) {}					
	}
*/	

	
	
	/**
	 * DrawnBlock-ok kirajzolasaert felelos osztaly
	 * 			
	 * @author akoel
	 *
	 */
	class DrawnBlockPainterListener implements PainterListener{

		@Override
		public void paintByWorldPosition(MCanvas canvas, MGraphics g2) {

			for( DrawnBlock sprite: drawnBlockList){
				sprite.draw(g2);
			}
			
		}
		
		@Override
		public void paintByCanvasAfterTransfer(MCanvas canvas, Graphics2D g2) {}
		
	}
	
	/**
	 * Az atmeneti retegben elhelyezett DrawnBlock-ok kirajzolasaert felelos osztaly
	 * 
	 * @author akoel
	 *
	 */
	class TemporaryDrawnBlockPainterListener implements PainterListener{

		@Override
		public void paintByWorldPosition(MCanvas canvas, MGraphics g2) {
			
			// Kirajzolja a Temporary listaban levo elemeket
			for( DrawnBlock drawnBlock: temporaryDrawnBlockList){
				drawnBlock.draw(g2);
			}
			
			//Majd a vegen torli a listat
			temporaryDrawnBlockList.clear();
		}		

		@Override
		public void paintByCanvasAfterTransfer(MCanvas canvas, Graphics2D g2) {}
		
	}
	
	/**
	 * Az Masodlagos kurzor kirajzolasaert felelos osztaly
	 * 
	 * @author afoldvarszky
	 *
	 */
	class TemporarySecondaryCursorPainterListener implements PainterListener{

		@Override
		public void paintByWorldPosition(MCanvas canvas, MGraphics g2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void paintByCanvasAfterTransfer( MCanvas canvas, Graphics2D g2 ) {
		
			secondaryCursor.draw(g2);
			
		}
		
	}
	
	/**
	 * Masodlagos kurzort megvalosito osztaly
	 * 
	 * @author afoldvarszky
	 *
	 */
	class SecondaryCursor{
		PositionValue cursorPosition = null;
		
		public SecondaryCursor(){
			this.cursorPosition = new PositionValue(0, 0);
		}
		
		public SecondaryCursor( PositionValue cursorPosition ){
			this.cursorPosition = new PositionValue( cursorPosition.getX(), cursorPosition.getY() );
		}
		
		public void setPosition( double x, double y ){
			this.cursorPosition.setX( x );
			this.cursorPosition.setY( y );
		}
		
		public void setPosition( PositionValue cursorPosition ){
			this.cursorPosition.setX( cursorPosition.getX() );
			this.cursorPosition.setY( cursorPosition.getY() );
		}
		
		public double getX(){
			return cursorPosition.getX();
		}
		
		public double getY(){
			return cursorPosition.getY();
		}
		
		public void draw( Graphics2D g2 ){
			int x, y;
			
			if( null != cursorPosition ){

				x = getPixelXPositionByWorldBeforeTranslate( cursorPosition.getX() );
				y = getPixelYPositionByWorldBeforeTranslate( cursorPosition.getY() );
			
				g2.setColor( Color.white );
				g2.setStroke( basicStroke );
				g2.drawLine( x, y - 8, x, y + 8 );
				g2.drawLine( x - 8, y, x + 8, y );
			}
			
		}
	}

}