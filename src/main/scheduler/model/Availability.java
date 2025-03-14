package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.*;

public class Availability {
    private Date d;
    private String caregiver;

    private Availability(AvailabilityGetter getter) {
        this.d = getter.d;
        this.caregiver = getter.caregiver;
    }

    private Availability(AvailabilityBuilder builder) {
        this.d = builder.d;
        this.caregiver = builder.caregiver;
    }

    public void addAvailability() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAvailability  = "INSERT INTO Availabilities VALUES (? , ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAvailability);
            statement.setDate( 1, this.d );
            statement.setString(2, this.caregiver);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void deleteAvailability() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String removeAvailability  = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(removeAvailability);
            statement.setDate( 1, this.d);
            statement.setString(2, this.caregiver);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AvailabilityGetter {
        private final Date d;
        private final String caregiver;

        public AvailabilityGetter(Date d, String caregiver) {
            this.d = d;
            this.caregiver = caregiver;
        }

        public Availability get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getAvailability = "SELECT * FROM Availabilities WHERE Time = ? AND Username = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getAvailability);
                statement.setDate(1, this.d);
                statement.setString(2, this.caregiver);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    return new Availability(this);
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }

    public static class AvailabilityBuilder {
        private final Date d;
        private final String caregiver;

        public AvailabilityBuilder(Date d, String caregiver) {
            this.d = d;
            this.caregiver = caregiver;
        }

        public Availability build() throws SQLException {
            return new Availability(this);
        }
    }
}
