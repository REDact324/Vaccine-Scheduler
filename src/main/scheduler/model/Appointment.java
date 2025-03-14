package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Appointment {
    private final int id;
    private final Date d;
    private final String caregiver;
    private final String patient;
    private final String vaccine;

    private Appointment(AppointmentGetter getter) {
        this.id = getter.id;
        this.d = getter.d;
        this.caregiver = getter.caregiver;
        this.patient = getter.patient;
        this.vaccine = getter.vaccine;
    }

    private Appointment(AppointmentBuilder builder) {
        this.id = builder.id;
        this.d = builder.d;
        this.caregiver = builder.caregiver;
        this.patient = builder.patient;
        this.vaccine = builder.vaccine;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAppointments = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointments);
            statement.setInt(1, this.id);
            statement.setDate(2, this.d);
            statement.setString(3, this.caregiver);
            statement.setString(4, this.patient);
            statement.setString(5, this.vaccine);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public String getCaregiver() {
        return this.caregiver;
    }

    public Date getD() {
        return this.d;
    }

    public String getVaccine() {
        return this.vaccine;
    }

    public void cancelAppointment() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String deleteAppointments = "DELETE FROM Appointments WHERE Id = ?";
        try{
            PreparedStatement statement = con.prepareStatement(deleteAppointments);
            statement.setInt(1, this.id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AppointmentGetter {
        private final int id;
        private Date d;
        private String caregiver;
        private String patient;
        private String vaccine;

        public AppointmentGetter(int id, Date d, String caregiver, String patient, String vaccine) {
            this.id = id;
            this.d = d;
            this.caregiver = caregiver;
            this.patient = patient;
            this.vaccine = vaccine;
        }

        public AppointmentGetter(int id) {
            this.id = id;
        }

        public Appointment get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getAvailability = "SELECT * FROM Appointments WHERE Id = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getAvailability);
                statement.setInt(1, this.id);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    int id = resultSet.getInt(1);
                    Date d = resultSet.getDate(2);
                    String caregiver = resultSet.getString(3);
                    String patient = resultSet.getString(4);
                    String vaccine = resultSet.getString(5);
                    AppointmentGetter getter = new AppointmentGetter(id, d, caregiver, patient, vaccine);
                    return new Appointment(getter);
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }

    public static class AppointmentBuilder {
        private final int id;
        private Date d;
        private String caregiver;
        private String patient;
        private String vaccine;

        public AppointmentBuilder(int id, Date d, String caregiver, String patient, String vaccine) {
            this.id = id;
            this.d = d;
            this.caregiver = caregiver;
            this.patient = patient;
            this.vaccine = vaccine;
        }

        public Appointment build() throws SQLException {
            return new Appointment(this);
        }
    }
}


