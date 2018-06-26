package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.ArrayList;
import java.util.List;

public class Boat2 {

	private static int adultsO, childrenO, adultsM, childrenM;
	private static BoatGrader bg;

	private enum Specie {Adult, Child}

	private static Specie Adult = Specie.Adult, Child = Specie.Child;

	private enum Location {Oahu, Molokai}
	private static Location Oahu = Location.Oahu, Molokai = Location.Molokai;

	private static Location boatLocation;
	private static List<Person> boat = new ArrayList<>();

	private static Lock boatLock, maybeLock;
	private static Condition adultCanGo, adultCannotGo,
					waitRider, waitRower, emptyBoat, maybeOver;

	private static boolean firstChildOnOahu, gameOver;

	public static void selfTest() {

		System.out.println("\n ***Testing Boats with only 2 children***");
		new BoatGrader().startTest(0, 2);

		System.out.println("\n ***Testing Boats with only 3 children***");
		new BoatGrader().startTest(0, 3);

		System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		new BoatGrader().startTest(1, 2);

		/*System.out.println("\n ***Testing Boats with 3 children, 3 adult***");
		new BoatGrader().startTest(3, 3);*/

		System.out.println("\n ***Testing Boats with 100 children, 1 adult***");
		new BoatGrader().startTest(0, 90);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		Lib.assertTrue(adults >= 0 && children >= 2);
		bg = b;

		adultsO = childrenO = adultsM = childrenM = 0;
		firstChildOnOahu = gameOver = false;
		boatLocation = Oahu;
		boatLock = new Lock();
		adultCanGo = new Condition(boatLock);
		adultCannotGo = new Condition(boatLock);
		emptyBoat = new Condition(boatLock);
		waitRider = new Condition(boatLock);
		waitRower = new Condition(boatLock);
		maybeLock = new Lock();
		maybeOver = new Condition(maybeLock);

		maybeLock.acquire();

		Person.num = Person.sum	= 0L;
		Lib.debug('b', "boat test start");
		for (int i = 0; i < adults; i++) {
			new KThread(new Person(Adult)).setName("Adult" + i).fork();
		}
		for (int i = 0; i < children; i++) {
			new KThread(new Person(Child)).setName("Child" + i).fork();
		}

		while (!gameOver) {
			maybeOver.sleep();
			if (childrenM + adultsM == adults + children) {
				gameOver = true;
			} else {
				emptyBoat.wake();
			}
		}
		maybeLock.release();

		System.out.println("Average Waiting Time = " + Person.sum / Person.num + " Ticks\n");
	}

	private static void AdultItinerary(Person adult) {
		++adultsO;
		addAdultToBoat(adult);
		bg.AdultRowToMolokai();
		adultRowToMolokai(adult);
	}

	private static void ChildItinerary(Person child) {
		++childrenO;
		while (!gameOver) {
			Long start = Machine.timer().getTime();
			if (child.location == Oahu) {
				if (isRowOrRide(child)) {
					bg.ChildRowToMolokai();
					childRowToMolokai(child);
				} else {
					bg.ChildRideToMolokai();
					childRideToMolokai(child);
				}
			} else {
				if (child.population > 0) {
					childReturnToBoat(child);
					bg.ChildRowToOahu();
					childRowToOahu(child);
				}
			}
			Long end = Machine.timer().getTime();
			Person.sum += end - start;
			++Person.num;
			KThread.yield();
		}
	}

	private static boolean isRowOrRide(Person child) {
		boatLock.acquire();
		if (!firstChildOnOahu) {
			firstChildOnOahu = true;
		} else {
			while (childrenO == 1 || boatLocation != Oahu || boat.size() >= 2) {
				adultCannotGo.sleep();
			}
		}
		boat.add(child);
		if (boat.size() == 2) {
			waitRider.wake();
			while (boat.size() == 2) {
				waitRower.sleep();
			}
			boatLock.release();
			return false;
		} else {
			while (boat.size() != 2) {
				waitRider.sleep();
			}
			boatLock.release();
			return true;
		}
	}

	private static void offBoat(Person person) {
		boatLock.acquire();
		boat.remove(person);
		if (boat.isEmpty()) {
			if (boatLocation == Molokai) {
				if (person.population == 0) {
					maybeLock.acquire();
					maybeOver.wake();
					maybeLock.release();
				} else {
					emptyBoat.wake();
				}
			} else {
				if (childrenO == 1) {
					adultCanGo.wake();
				} else {
					adultCannotGo.wake();
				}
			}
		}
		boatLock.release();
	}

	private static void childRowToMolokai(Person child) {
		child.population = childrenO + adultsO - 2;
		--childrenO;
		++childrenM;
		boatLocation = Molokai;
		offBoat(child);
		child.location = Molokai;
		boatLock.acquire();
		waitRower.wake();
		boatLock.release();
	}

	private static void childRideToMolokai(Person child) {
		child.population = childrenO + adultsO - 1;
		--childrenO;
		++childrenM;
		boatLocation = Molokai;
		offBoat(child);
		child.location = Molokai;
	}

	private static void childReturnToBoat(Person child) {
		boatLock.acquire();
		while (boatLocation != Molokai || !boat.isEmpty() || gameOver) {
			emptyBoat.sleep();
		}
		boat.add(child);
		boatLock.release();
	}

	private static void childRowToOahu(Person child) {
		child.population = childrenM + adultsM - 1;
		--childrenM;
		++childrenO;
		boatLocation = Oahu;
		offBoat(child);
		child.location = Oahu;
	}

	private static void addAdultToBoat(Person adult) {
		boatLock.acquire();
		while (childrenO != 1 || boatLocation != Oahu || !boat.isEmpty() || !firstChildOnOahu) {
			adultCanGo.sleep();
		}
		boat.add(adult);
		boatLock.release();
	}

	private static void adultRowToMolokai(Person adult) {
		adult.population = childrenO + adultsO - 1;
		--adultsO;
		++adultsM;
		boatLocation = Molokai;
		offBoat(adult);
		adult.location = Molokai;
	}

	private static class Person implements Runnable {
		Specie specie;
		Location location;
		int population;
		static Long num, sum;
		Person(Specie specie) {
			this.specie = specie;
			this.location = Oahu;
		}

		@Override
		public void run() {
			if (specie == Adult) {
				AdultItinerary(this);
			}
			if (specie == Child) {
				ChildItinerary(this);
			}
		}
	}
}
