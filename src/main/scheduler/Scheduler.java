package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.model.Appointment;
import scheduler.model.Availability;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import scheduler.db.ConnectionManager;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // Initialize the database if no tables exist
        ConnectionManager cm = new ConnectionManager();
        Connection conn = cm.createConnection();
        String sqlFilePath = Paths.get(
                Paths.get("").toAbsolutePath().toString(),
                "src", "main", "resources", "sqlite", "create.sql"
        ).toString();
        try {
            Statement stmt = conn.createStatement();
            // Query for tables in the database information schema
            ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table';");
            if (!rs.next()) {  // No tables exist
                String sqlScript = new String(Files.readAllBytes(Paths.get(sqlFilePath)));
                // Run create.sql script against the database
                stmt.executeUpdate(sqlScript);
            }
        } catch (SQLException e) {
            System.out.println("Error initializing database tables: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + sqlFilePath + " not found.");
        } finally {
            cm.closeConnection();
        }

        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static boolean checkPasswordStrength(String password) {
        Pattern p = Pattern.compile("(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#?])(?=.{8,})");
        Matcher m = p.matcher(password);
        return m.find();
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Create patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        if (!checkPasswordStrength(password)) {
            System.out.println("Create patient failed, please use a strong password (8+ char, at least one upper and one" +
                    " lower, at least one letter and one number, and at least one special character, " +
                    "from \"!\", \"@\", \"#\", \"?\")");
            return;
        }

        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create patient failed");
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Create caregiver failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        if (!checkPasswordStrength(password)) {
            System.out.println("Create caregiver failed, please use a strong password (8+ char, at least one upper and one" +
                    " lower, at least one letter and one number, and at least one special character, " +
                    "from \"!\", \"@\", \"#\", \"?\")");
            return;
        }

        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create caregiver failed.");
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in, try again");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login patient failed");
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login patient failed");
        } else {
            System.out.println("Logged in as " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in, try again.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login caregiver failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login caregiver failed.");
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login caregiver failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void printCaregiverSchedule(Date date){
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectSchedule = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC";
        try {
            PreparedStatement statement = con.prepareStatement(selectSchedule);
            statement.setDate(1, date);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()){
                System.out.println("No caregivers available");
                return;
            }
            do {
                String caregiver = resultSet.getString(1);
                System.out.println(caregiver);
            } while (resultSet.next());
        } catch (SQLException e) {
            System.out.println("Please try again");
        } finally {
            cm.closeConnection();
        }
    }

    private static void printVaccineAvailability(){
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectSchedule = "SELECT * FROM Vaccines WHERE Doses > 0";
        try {
            PreparedStatement statement = con.prepareStatement(selectSchedule);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()){
                System.out.println("No vaccines available");
                return;
            }
            do {
                String vaccine = resultSet.getString(1);
                String dose = resultSet.getString(2);
                System.out.println(vaccine + " " + dose);
            } while (resultSet.next());
        } catch (SQLException e) {
            System.out.println("Please try again");
        } finally {
            cm.closeConnection();
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }

        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }

        String date = tokens[1];
        Date d;

        try{
            d = Date.valueOf(date);
        } catch (Exception e) {
            System.out.println("Please try again");
            return;
        }

        System.out.println("Caregivers:");
        printCaregiverSchedule(d);

        System.out.println("Vaccines:");
        printVaccineAvailability();

    }

    private static String searchAvailableCaregiver(Date date){
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectSchedule = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC";
        try {
            PreparedStatement statement = con.prepareStatement(selectSchedule);
            statement.setDate(1, date);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()){
                System.out.println("No caregiver is available");
                return null;
            }

            return resultSet.getString(1);
        } catch (SQLException e) {
            System.out.println("Please try again");
            return null;
        } finally {
            cm.closeConnection();
        }
    }

    private static int searchAvailableVaccine(String vaccine){
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectVaccine = "SELECT Doses FROM Vaccines WHERE Doses > 0 AND Name = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectVaccine);
            statement.setString(1, vaccine);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()){
                return 0;
            }

            return resultSet.getInt(1);
        } catch (SQLException e) {
            return 0;
        } finally {
            cm.closeConnection();
        }
    }

    private static int getLastId() {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectSchedule = "SELECT MAX(Id) FROM Appointments";
        try {
            PreparedStatement statement = con.prepareStatement(selectSchedule);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()){
                return 0;
            }
            return resultSet.getInt(1);
        } catch (SQLException e) {
            System.out.println("Please try again");
            return -1;
        } finally {
            cm.closeConnection();
        }
    }

    private static void updateAppointment(int id, Date date, String caregiver, String patient, String vaccine) {
        try {
            Appointment.AppointmentBuilder builder = new Appointment.AppointmentBuilder(id, date, caregiver, patient, vaccine);
            Appointment appointment = builder.build();
            appointment.saveToDB();
            System.out.println("Appointment ID " + String.valueOf(id) + ", Caregiver username " + caregiver);
        } catch (SQLException e) {
            System.out.println("Please try again");
            return;
        }
    }

    private static void reserve(String[] tokens) {
        if (currentPatient == null) {
            if (currentCaregiver == null) {
                System.out.println("Please login first");
                return;
            }
            System.out.println("Please login as a patient");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Please try again");
            return;
        }

        String date = tokens[1];
        String vaccine = tokens[2];
        Date d;

        try {
            d = Date.valueOf(date);
        } catch (Exception e) {
            System.out.println("Please try again");
            return;
        }

        String caregiver = searchAvailableCaregiver(d);
        if (caregiver == null) { return; }

        int doses = searchAvailableVaccine(vaccine);
        if (doses == 0) {
            System.out.println("Not enough available doses");
            return;
        }

        int id = getLastId();
        if (id == -1) { return; }
        id = id + 1;

        updateAppointment(id, d, caregiver, currentPatient.getUsername(), vaccine);

        try {
            Availability.AvailabilityGetter a_getter = new Availability.AvailabilityGetter(d, caregiver);
            Availability availability = a_getter.get();
            availability.deleteAvailability();

            Vaccine.VaccineGetter v_getter = new Vaccine.VaccineGetter(vaccine);
            Vaccine v = v_getter.get();
            v.decreaseAvailableDoses(1);
        } catch (SQLException e) {
            System.out.println("Please try again");
            return;
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
        }
    }

    private static void cancel(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }

        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }

        int id = Integer.parseInt(tokens[1]);

        Appointment.AppointmentGetter getter = new Appointment.AppointmentGetter(id);

        try{
            Appointment appointment = getter.get();
            if (appointment == null) {
                System.out.println("Appointment ID " + tokens[1] + " does not exist");
                return;
            }
            String caregiver = appointment.getCaregiver();
            Date d = appointment.getD();
            String vaccine = appointment.getVaccine();

            appointment.cancelAppointment();

            Availability.AvailabilityBuilder a_builder = new Availability.AvailabilityBuilder(d, caregiver);
            Availability availability = a_builder.build();
            availability.addAvailability();

            Vaccine.VaccineGetter v_getter = new Vaccine.VaccineGetter(vaccine);
            Vaccine v = v_getter.get();
            v.increaseAvailableDoses(1);

            System.out.println("Appointment ID " + tokens[1] + " has been successfully canceled");
        } catch (SQLException e) {
            System.out.println("Please try again");
            return;
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
            }
        }
        System.out.println("Doses updated!");
    }

    private static void getAllAppointments(String username, boolean isCaregiver) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectAppointments;
        if (isCaregiver) { selectAppointments = "SELECT Id, Time, P_username, V_name FROM Appointments WHERE C_username = ?"; }
        else { selectAppointments = "SELECT Id, Time, C_username, V_name FROM Appointments WHERE P_username = ?"; }

        try {
            PreparedStatement statement = con.prepareStatement(selectAppointments);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                System.out.println("No appointments scheduled");
                return;
            }
            do {
                String id = String.valueOf(resultSet.getInt(1));
                Date date = resultSet.getDate(2);
                String name = resultSet.getString(3);
                String vaccine = resultSet.getString(4);
                System.out.println(id + " "  + vaccine + " " + date + " " + name);
            } while (resultSet.next());
        } catch (SQLException e) {
            System.out.println("Please try again");
            return;
        } finally {
            cm.closeConnection();
        }
    }

    private static void showAppointments(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }

        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }

        String username;
        boolean isCaregiver;

        if (currentCaregiver != null) {
            username = currentCaregiver.getUsername();
            isCaregiver = true;
        } else {
            username = currentPatient.getUsername();
            isCaregiver = false;
        }

        getAllAppointments(username, isCaregiver);
    }

    private static void logout(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }

        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }

        try {
            if (currentCaregiver != null) { currentCaregiver = null; }
            if (currentPatient != null) { currentPatient = null; }
            System.out.println("Successfully logged out");
        } catch (Exception e) {
            System.out.println("Please try again");
            return;
        }
    }
}
