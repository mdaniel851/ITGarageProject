/*
 * This class is used to store information regarding the robot used for battle.
 * It is also used to compute the attack damage and return it to the calling 
 * class, also it is used to record damage taken on any given turn.  The cool
 * downs for the special moves (charge, heal) are recorded here and used to 
 * determine if a given action is allowed on a given turn.
*/

package servers;
import java.util.Random;

public class Robot{

	private int 	attack 		= 10; 
	private int 	defend 		= 5;
	private int 	cool 		= 0; 
	private int 	coolh 		= 0;
	private int 	health 		= 50;
	private Random 	random 		= new Random();
	private String 	action 		= "";
	private int 	maxHealth;

	// Constructor
	public Robot(int[] sA){

		this.attack = sA[0];
		this.defend = sA[1];
		this.health += this.defend * 2;
		this.maxHealth = (50 + this.defend);
		this.random.setSeed(System.nanoTime());
	}

	// Attack Defend, Heal, Charged Attack ------------------------------------

	/*
         * The following functions are used to compute/return and record 
	 * the damage taken and returned on any given round of the match.
	 * The cool downs are taken into account to check if a given action 
	 * is allowed.	
	*/

	/*
	 * This is the regular defence method.  It subtracts the base defence and 
	 * roll of the dice from the incoming attack value.  The resulting damage 
	 * is recorded in the stats
	 *
	 * Input: integer attack value aV 
	 * Output: none
	*/

	private void defends(int aV){

		if ( cool > 0 ) {
			cool -= 1;
		}
		if ( coolh > 0 ) {
			coolh -= 1;
		}
		int damage = aV - defend - rollDice();
    
		if ( damage > 0 ) {
			health -= damage;
		}
	}

	/*
	 * This is method computes the result of a heal action.  It subtracts the 
	 * base defence and heal value from the incoming attack value.  The resulting damage 
	 * is recorded in the stats.  Since this is a special, cool downs are checked.  If
	 * a special is not allowed the regular is used (defend).
	 *
	 * Input: integer attack value aV 
	 * Output: none
	*/

	private void heal(int damage){

		int healValue = this.defend + rollDice();

		if ( cool > 0 ) {
			cool -= 1;
		}
		if ( coolh < 1 ) {
			coolh = 2;

			if ( healValue + health < maxHealth ) {
				health += healValue - damage;
			} else {
				health = (maxHealth - damage);
			}
		} else {
			defends( damage );
		}
	}


   	/*
	 * No defence is played, this happens when attacking.
	 *
	 * Input: integer attack value aV 
	 * Output: none
	*/

	private void defenceless( int aV ){

		health -= aV;
	}
 

	/*
	 * This is the regular attack method.  The attack valuse is returned.
	 *
	 * Input: none
	 * Output: returns integer attack value
	*/
	
	private int attack(){
		if ( coolh > 0 )
			coolh -= 1;
		if ( cool > 0 ) {
			cool -= 1;
		}
			return attack;
		}

	/*
	 * This is the special attack method.  It returns the base attack plus
	 * the roll of the dice if not on a cool down, otherwise return base attack
	 *	
	 * Input: none 
	 * Output: returns integer attack value
	*/  

	private int chargeAttack(){
		if ( coolh > 0 ) {
			coolh -= 1;
		}
		if ( cool < 1 ) {
			cool = 2;
			return attack + rollDice();
		}
	   
			cool -= 1;
			return attack;
	}


	// Decision Methods -------------------------------------------------------
	
	/*
	 * These methods are used to decide which of the actions are to be taken.
	 * 
	*/

	public void attackedBy(int attackValue){
		
		switch( action ){

			case "heal":	heal(attackValue);
					break;
	
			case "defend": 	defends(attackValue);
					break;

			default:	defenceless(attackValue);
		}		
	}
  

	public int attacks(){

		int damage;
	
		switch( action ){

			case "charge": 	damage = chargeAttack();
					break;

			case "attack": 	damage = attack();
					break;

			default: 	damage = 0;

		}

		return damage;
	}

   
	// Utilities ---------------------------------------------------------------
	
	/*
		This function is used to produce a random value to be added to
		the attack/ defend stat

		Input: no input
		Output: returns a random int between 1 and 6
	*/

	private int rollDice(){
		return 1 + random.nextInt( 6 );
	}

   	/*
		This function is used to wipe the stats for this robot after
		a match.
		
		No input or output
	*/

	public void reset(){

		this.cool = 0;
		this.coolh = 0;
		this.health = 20;
	}
   

	// Getter Setter ------------------------------------------------------------

	public boolean isAlive(){
		return health > 0;
	}
   
	public int getAttackStat(){
		return attack;
	}
   
	public void setAction(String a) {
		action = a;
	}
	   
	public String getCoolDowns() { 
		return cool + " " + this.coolh; 
	}
   
	public int getHealth(){
		return health;
	}   

	public int getCool() {
		return cool;
	}
   
	public int getCoolHeal() {
		return coolh;
	}
   
	public String toString() {
		return attack + "," + defend;
	}
}
