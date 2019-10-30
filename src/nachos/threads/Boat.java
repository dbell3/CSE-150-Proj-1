package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat {
	static BoatGrader bg;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		// System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		// begin(1, 2, b);

		// System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		// begin(3, 3, b);

	}

	// Set-up state
	static final int Adult = 2; // an adult takes up 2 points of boat's capacity
	static final int Child = 1; // child takes 1 point of boat's capacity

	static boolean Done = false; // finishes the thread. (When Makes it true)

	static Lock OahuPopulation;// setting up the population on Oahu
	static int AmmAdultsAtOahu = 0; // Ammount of adults at Oahu
	static int AmmChildrenAtOahu = 0; // Ammount of children at Oahu

	static Lock PermitOnBoat; // access to boat
	static boolean BoatAtOahu = true; // true: boat is at Oahu; false: boat is at Molokai
	static int BoatCapacity = 0; // boat is empty (max capacity = 2 children or 1 adult)
	static boolean AllowAdult = false; // adult may us boat only if it is free
	static Alarm BlockSync; // blocks syncranization when spawning.

	static Condition Spawn;
	static Condition Finished;

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here
		OahuPopulation = new Lock();
		PermitOnBoat = new Lock();
		BlockSync = new Alarm();
		Spawn = new Condition(OahuPopulation);
		Finished = new Condition(PermitOnBoat);
		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		//
		// Runnable r = new Runnable() {
		// public void run() {
		// SampleItinerary();
		// }
		// };
		// KThread t = new KThread(r);
		// t.setName("Sample Boat Thread");
		// t.fork();

		// Set up two separated threads (For child and adult respectivly)
		Runnable A = new Runnable() {
			public void run() {
				AdultItinerary();
			}
		};
		Runnable C = new Runnable() {
			public void run() {
				ChildItinerary();
			}
		};

		boolean intStatus = Machine.interrupt().disable(); // prevent context switch until done spawning

		for (int i = 0; i < adults; ++i) {
			KThread t = new KThread(A);
			t.fork();
		}

		for (int i = 0; i < children; ++i) {
			KThread t = new KThread(C);
			t.fork();
		}

		Machine.interrupt().restore(intStatus); // spawning done

		while (!Done)
			KThread.yield(); // wait until forked threads are done
	}

	static void AdultItinerary() {
		/*
		 * This is where you should put your solutions. Make calls to the BoatGrader to
		 * show that it is synchronized. For example: bg.AdultRowToMolokai(); indicates
		 * that an adult has rowed the boat across to Molokai
		 */
		boolean AtOahu = true; // thread start at Oahu
		OahuPopulation.acquire();
		AmmAdultsAtOahu++; // increment more adults
		Spawn.sleep();
		OahuPopulation.release();

		BlockSync.waitUntil(100);// block syncranization
		KThread.yield(); // children go first

		while (!Done) // nicely looped until done
		{
			PermitOnBoat.acquire();// setting up the boat logic for adults (From psudocode)
			if (BoatAtOahu && AtOahu && BoatCapacity == 0 && (AllowAdult || AmmChildrenAtOahu == 0) && !Done
					&& AmmChildrenAtOahu < 2) {
				bg.AdultRowToMolokai(); // go to Molokai

				OahuPopulation.acquire();
				AmmAdultsAtOahu--;// reduce number of adults on Oahu
				OahuPopulation.release();
				if ((AmmAdultsAtOahu + AmmChildrenAtOahu) == 0)// if no one is left
				{
					Done = true;// we are done here
					Finished.wakeAll();
				}
				AtOahu = false;// we are not at Oahu
				BoatAtOahu = false;// there is no bat at Oahu
				AllowAdult = false;// no one it to allow
			}
			if (!Done && !AtOahu)
				Finished.sleep(); // pull asleep
			PermitOnBoat.release();// relace boat
		}
		return; // done with adult thread set up
	}

	static void ChildItinerary() {
		// Almost the same set up as for the adult
		boolean AtOahu = true; // thread start at Oahu

		OahuPopulation.acquire();
		AmmChildrenAtOahu++; // increment more children

		OahuPopulation.release();

		BlockSync.waitUntil(100);// block syncranization

		while (!Done) // nicely looped until done
		{
			PermitOnBoat.acquire();// setting up the boat logic for adults (From psudocode)
			if (BoatAtOahu && AtOahu && BoatCapacity < 2 && !Done && (AllowAdult || AmmChildrenAtOahu > 0)) {
				BoatCapacity++; // board the boat
				OahuPopulation.acquire();
				AmmChildrenAtOahu--; // subtract from the population of children at Oahu
				OahuPopulation.release();
				AtOahu = false; // no longer at Oahu
				if ((AmmAdultsAtOahu + AmmChildrenAtOahu) == 0) // if Oahu population = 0
				{
					Done = true; // flag other threads to finish
					bg.ChildRowToMolokai(); // leave to Molokai
					if (BoatCapacity == 2) // if boat has 2 children in it
					{
						bg.ChildRideToMolokai(); // passenger has also left to Molokai
					}
					BoatCapacity = 0; // no one on boat
					BoatAtOahu = false; // boat is now at Molokai
					Finished.wakeAll();
				} else if (BoatCapacity == 2) // if boat is full
				{
					bg.ChildRowToMolokai(); // both children
					bg.ChildRideToMolokai(); // go to Molokai
					BoatCapacity = 0; // empty out boat
					BoatAtOahu = false; // boat is now at Molokai
				} else if (AmmChildrenAtOahu == 0) // if no children left at Oahu
				{
					// stay on Oahu (because 1 child must be the last to leave Oahu)
					BoatCapacity--; // empty out boat
					OahuPopulation.acquire();
					AmmChildrenAtOahu++; // add to the population of children at Oahu
					OahuPopulation.release();
					AtOahu = true; // staying at Oahu still
				}
			} else if (!BoatAtOahu && !AtOahu && BoatCapacity < 2 && !Done) {
				bg.ChildRowToOahu(); // go back to Oahu
				AtOahu = true; // now at Oahu
				OahuPopulation.acquire();
				Spawn.wakeAll();
				AmmChildrenAtOahu++; // add to the population of children at Oahu
				OahuPopulation.release();
				BoatAtOahu = true; // boat is now at Oahu
				AllowAdult = true; // flag that an adult may go to Molokai
			}
			PermitOnBoat.release(); // release boat
			if (!Done)
				KThread.yield(); // wait for another thread
		}
		return;
	}

	static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}

}
