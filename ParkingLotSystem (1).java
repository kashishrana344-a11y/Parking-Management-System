import java.io.*;
import java.util.*;

// ---------- Interface ----------
interface Billable {
    double calculateFee(long hours);
}

// ---------- Custom Exceptions ----------
class ParkingFullException extends Exception {
    ParkingFullException(String msg) { super(msg); }
}

class InvalidTicketException extends Exception {
    InvalidTicketException(String msg) { super(msg); }
}

// ---------- Inheritance + Polymorphism ----------
abstract class Vehicle implements Billable {
    protected String number;

    Vehicle(String number) { this.number = number; }

    String getNumber() { return number; }

    abstract String getType();
}

class Car extends Vehicle {
    Car(String number) { super(number); }

    String getType() { return "Car"; }

    public double calculateFee(long hours) { return hours * 20; }
}

class Bike extends Vehicle {
    Bike(String number) { super(number); }

    String getType() { return "Bike"; }

    public double calculateFee(long hours) { return hours * 10; }
}

// ---------- Ticket ----------
class Ticket {
    private static int counter = 1000;
    int id;
    Vehicle vehicle;
    int slotNo;
    long entryTime;

    Ticket(Vehicle vehicle, int slotNo) {
        this.id = ++counter;
        this.vehicle = vehicle;
        this.slotNo = slotNo;
        this.entryTime = System.currentTimeMillis();
    }
}

// ---------- Parking Lot (core logic) ----------
class ParkingLot {
    private final int capacity;
    private final Ticket[] slots;
    private final Map<Integer, Ticket> activeTickets = new HashMap<>();
    private final List<String> history = new ArrayList<>();

    ParkingLot(int capacity) {
        this.capacity = capacity;
        this.slots = new Ticket[capacity];
    }

    synchronized Ticket park(Vehicle v) throws ParkingFullException {
        for (int i = 0; i < capacity; i++) {
            if (slots[i] == null) {
                Ticket t = new Ticket(v, i + 1);
                slots[i] = t;
                activeTickets.put(t.id, t);
                saveToFile("ENTRY | Ticket " + t.id + " | " + v.getType() + " " + v.getNumber() + " | Slot " + t.slotNo);
                return t;
            }
        }
        throw new ParkingFullException("Parking full! Koi slot khali nahi hai.");
    }

    synchronized double exit(int ticketId) throws InvalidTicketException {
        Ticket t = activeTickets.remove(ticketId);
        if (t == null) throw new InvalidTicketException("Invalid ticket ID: " + ticketId);

        long minutes = (System.currentTimeMillis() - t.entryTime) / 60000;
        long hours = Math.max(1, (long) Math.ceil(minutes / 60.0)); // minimum 1 hour
        double fee = t.vehicle.calculateFee(hours); // polymorphism
        slots[t.slotNo - 1] = null;
        saveToFile("EXIT  | Ticket " + t.id + " | " + t.vehicle.getNumber() + " | Fee Rs." + fee);
        return fee;
    }

    synchronized void showSlots() {
        System.out.println("\n--- Slot Status ---");
        for (int i = 0; i < capacity; i++) {
            if (slots[i] == null)
                System.out.println("Slot " + (i + 1) + ": FREE");
            else
                System.out.println("Slot " + (i + 1) + ": " + slots[i].vehicle.getType()
                        + " " + slots[i].vehicle.getNumber() + " (Ticket " + slots[i].id + ")");
        }
    }

    synchronized int occupiedCount() { return activeTickets.size(); }

    int getCapacity() { return capacity; }

    // ---------- File Handling ----------
    private void saveToFile(String line) {
        try (FileWriter fw = new FileWriter("tickets.txt", true)) {
            fw.write(new Date() + " | " + line + "\n");
        } catch (IOException e) {
            System.out.println("File error: " + e.getMessage());
        }
    }
}

// ---------- Multithreading: background status logger ----------
class StatusMonitor extends Thread {
    private final ParkingLot lot;

    StatusMonitor(ParkingLot lot) {
        this.lot = lot;
        setDaemon(true); // program band hote hi thread band
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(5000);
                try (FileWriter fw = new FileWriter("status.log", true)) {
                    fw.write(new Date() + " | Occupied: " + lot.occupiedCount()
                            + "/" + lot.getCapacity() + "\n");
                }
            } catch (InterruptedException | IOException e) {
                return;
            }
        }
    }
}

// ---------- Main (console menu) ----------
public class ParkingLotSystem {
    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot(5);
        new StatusMonitor(lot).start();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n===== SMART PARKING SYSTEM =====");
            System.out.println("1. Park Vehicle");
            System.out.println("2. Exit Vehicle (pay fee)");
            System.out.println("3. Show Slots");
            System.out.println("4. Exit Program");
            System.out.print("Choice: ");

            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please number enter karo!");
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.print("Vehicle type (1=Car, 2=Bike): ");
                    String type = sc.nextLine().trim();
                    System.out.print("Vehicle number: ");
                    String num = sc.nextLine().trim().toUpperCase();
                    Vehicle v = type.equals("1") ? new Car(num) : new Bike(num);
                    try {
                        Ticket t = lot.park(v);
                        System.out.println("Parked! Ticket ID: " + t.id + ", Slot: " + t.slotNo);
                    } catch (ParkingFullException e) {
                        System.out.println(e.getMessage());
                    }
                    break;
                case 2:
                    System.out.print("Ticket ID: ");
                    try {
                        int id = Integer.parseInt(sc.nextLine().trim());
                        double fee = lot.exit(id);
                        System.out.println("Vehicle exit ho gaya. Fee: Rs." + fee);
                    } catch (NumberFormatException e) {
                        System.out.println("Ticket ID number hona chahiye!");
                    } catch (InvalidTicketException e) {
                        System.out.println(e.getMessage());
                    }
                    break;
                case 3:
                    lot.showSlots();
                    break;
                case 4:
                    System.out.println("Thank you!");
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }
}
