package datamodel;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DataModel {
    private static final String TABLE_TASKS = "tasks";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SHORT_DESCRIPTION = "short_description";
    private static final String COLUMN_LONG_DESCRIPTION = "long_description";
    private static final String COLUMN_DUE_DATE = "due_date";
    private static final String COLUMN_COMPLETE = "complete";

    public static final String INSERT_TASK = "INSERT INTO " + TABLE_TASKS + "(" +
            COLUMN_SHORT_DESCRIPTION + ", " + COLUMN_LONG_DESCRIPTION + ", " + COLUMN_COMPLETE + ", " + COLUMN_DUE_DATE + ") VALUES(?, ?, ?, ?)";

    public static final String DELETE_TASK = "DELETE FROM " + TABLE_TASKS + " WHERE " + COLUMN_ID + "= ?";

    public static final String UPDATE_TASK = "UPDATE " + TABLE_TASKS + " SET " + COLUMN_SHORT_DESCRIPTION + " = ?, "
            + COLUMN_LONG_DESCRIPTION + " = ?, " + COLUMN_DUE_DATE + " = ? WHERE " + COLUMN_ID + " = ? ";

    public static final String COUNT_TASKS = "SELECT COUNT(" + COLUMN_ID +
            ") FROM " + TABLE_TASKS;

    public static final String UPDATE_COMPLETE = "UPDATE " + TABLE_TASKS + " SET " + COLUMN_COMPLETE + " = ? WHERE " + COLUMN_ID + " = ?";

    public static final String COUNT_COMPLETED_TASKS = "SELECT COUNT(" + COLUMN_ID
            + ") FROM " + TABLE_TASKS + " WHERE complete = 1";
    private PreparedStatement insertTask;

    private PreparedStatement deleteTask;

    private PreparedStatement updateTask;

    private PreparedStatement updateComplete;
    Connection conn;
    private static final DataModel instance = new DataModel();
    private final DateTimeFormatter formatter;

    private DataModel() {
        formatter = DateTimeFormatter.ofPattern("d MMMM, yyyy");
    }


    public static DataModel getModel() {
        return instance;
    }


    public boolean open() {

        String url = "jdbc:sqlite:todolist.db";

        try {
            conn = DriverManager.getConnection(url);
            String query = """
                    CREATE TABLE IF NOT EXISTS tasks (
                     id integer PRIMARY KEY,
                     short_description text NOT NULL,
                     long_description text NOT NULL,\s
                     due_date text NOT NULL,  complete integer NOT NULL\s
                    );""";
            Statement statement = conn.createStatement();
            statement.execute(query);
            insertTask = conn.prepareStatement(INSERT_TASK);
            deleteTask = conn.prepareStatement(DELETE_TASK);
            updateTask = conn.prepareStatement(UPDATE_TASK);
            updateComplete = conn.prepareStatement(UPDATE_COMPLETE);
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public void close() {
        try {
            if (insertTask != null) {
                insertTask.close();
            }
            if (deleteTask != null) {
                deleteTask.close();
            }
            if (updateTask != null) {
                updateTask.close();
            }
            if (updateComplete != null) {
                updateComplete.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public ObservableList<TodoItem> queryTask() {
        StringBuilder sb = new StringBuilder("SELECT * FROM ");
        sb.append(TABLE_TASKS);
        sb.append(" ORDER BY complete ASC, due_date ASC");
        Statement statement = null;
        ResultSet results = null;
        try {
            statement = conn.createStatement();
            results = statement.executeQuery(sb.toString());

            List<TodoItem> tasks = new ArrayList<>();
            while (results.next()) {
                TodoItem task = new TodoItem();
                task.setId(results.getInt(COLUMN_ID));
                task.setShortDescription(results.getString(COLUMN_SHORT_DESCRIPTION));
                task.setDetailDescription(results.getString(COLUMN_LONG_DESCRIPTION));
                LocalDate date = LocalDate.parse(results.getString(COLUMN_DUE_DATE), formatter);
                task.setDueDate(date);
                task.setComplete(results.getInt(COLUMN_COMPLETE));
                tasks.add(task);
            }
            return FXCollections.observableArrayList(tasks);
        } catch (SQLException e) {
            System.out.println("Task query failed " + e.getMessage());
            return null;
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
            } catch (SQLException e) {
                // Oh no, anyway...
                System.out.println("Errors closing results: " + e.getMessage());
            }
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                // This was not supposed to happen
                System.out.println("Errors closing statement: " + e.getMessage());
            }
        }
    }

    public void insertTask(String shortDescription, String longDescription, LocalDate date) {
        try {
            conn.setAutoCommit(false);
            insertTask.setString(1, shortDescription);
            insertTask.setString(2, longDescription);
            insertTask.setInt(3, 0);
            String newDate = formatter.format(date);
            insertTask.setString(4, newDate);
            int affectedRows = insertTask.executeUpdate();
            if (affectedRows == 1) {
                conn.commit();
            } else {
                throw new SQLException("Task insert failed");
            }

        } catch (Exception e) {
            System.out.println("Insert Task failed : " + e.getMessage());
            try {
                System.out.println("Performing rollback");
                conn.rollback();
            } catch (SQLException e2) {
                System.out.println("Rollback failed, that's really bad : " + e2.getMessage());
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Could not reset autocommit : " + e.getMessage());
            }
        }
    }

    public ObservableList<TodoItem> queryTodayTask() {
        StringBuilder sb = new StringBuilder("SELECT * FROM ");
        sb.append(TABLE_TASKS);
        String today = formatter.format(LocalDate.now());
        sb.append(" WHERE due_date = ");
        sb.append("'");
        sb.append(today);
        sb.append("'");
        sb.append(" ORDER BY complete DESC");
        Statement statement = null;
        ResultSet results = null;
        try {
            statement = conn.createStatement();
            results = statement.executeQuery(sb.toString());

            List<TodoItem> tasks = new ArrayList<>();
            while (results.next()) {
                TodoItem task = new TodoItem();
                task.setId(results.getInt(COLUMN_ID));
                task.setShortDescription(results.getString(COLUMN_SHORT_DESCRIPTION));
                task.setDetailDescription(results.getString(COLUMN_LONG_DESCRIPTION));
                LocalDate date = LocalDate.parse(results.getString(COLUMN_DUE_DATE), formatter);
                task.setDueDate(date);
                task.setComplete(results.getInt(COLUMN_COMPLETE));
                tasks.add(task);
            }
            return FXCollections.observableArrayList(tasks);
        } catch (SQLException e) {
            System.out.println("Tasks query failed " + e.getMessage());
            return null;
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
            } catch (SQLException e) {
                // Oh no, anyway...
                System.out.println("Errors closing results: " + e.getMessage());
            }
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                // This was not supposed to happen
                System.out.println("Errors closing statement: " + e.getMessage());
            }
        }
    }

    public ObservableList<TodoItem> queryTodayCompleteTask() {
        StringBuilder sb = new StringBuilder("SELECT * FROM ");
        sb.append(TABLE_TASKS);
        String today = formatter.format(LocalDate.now());
        sb.append(" WHERE due_date = ");
        sb.append("'");
        sb.append(today);
        sb.append("'");
        sb.append(" AND complete = 0 ORDER BY complete DESC");
        Statement statement = null;
        ResultSet results = null;
        try {
            statement = conn.createStatement();
            results = statement.executeQuery(sb.toString());

            List<TodoItem> tasks = new ArrayList<>();
            while (results.next()) {
                TodoItem task = new TodoItem();
                task.setId(results.getInt(COLUMN_ID));
                task.setShortDescription(results.getString(COLUMN_SHORT_DESCRIPTION));
                task.setDetailDescription(results.getString(COLUMN_LONG_DESCRIPTION));
                LocalDate date = LocalDate.parse(results.getString(COLUMN_DUE_DATE), formatter);
                task.setDueDate(date);
                task.setComplete(results.getInt(COLUMN_COMPLETE));
                tasks.add(task);
            }
            return FXCollections.observableArrayList(tasks);
        } catch (SQLException e) {
            System.out.println("Tasks query failed " + e.getMessage());
            return null;
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
            } catch (SQLException e) {
                // Oh no, anyway...
                System.out.println("Errors closing results: " + e.getMessage());
            }
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                // This was not supposed to happen
                System.out.println("Errors closing statement: " + e.getMessage());
            }
        }
    }

    public ObservableList<TodoItem> queryCompleteTask() {
        StringBuilder sb = new StringBuilder("SELECT * FROM ");
        sb.append(TABLE_TASKS);
        sb.append(" WHERE complete = 0");
        sb.append(" ORDER BY due_date ASC");
        Statement statement = null;
        ResultSet results = null;
        try {
            statement = conn.createStatement();
            results = statement.executeQuery(sb.toString());

            List<TodoItem> tasks = new ArrayList<>();
            while (results.next()) {
                TodoItem task = new TodoItem();
                task.setId(results.getInt(COLUMN_ID));
                task.setShortDescription(results.getString(COLUMN_SHORT_DESCRIPTION));
                task.setDetailDescription(results.getString(COLUMN_LONG_DESCRIPTION));
                LocalDate date = LocalDate.parse(results.getString(COLUMN_DUE_DATE), formatter);
                task.setDueDate(date);
                task.setComplete(results.getInt(COLUMN_COMPLETE));
                tasks.add(task);
            }
            return FXCollections.observableArrayList(tasks);
        } catch (SQLException e) {
            System.out.println("Tasks query failed " + e.getMessage());
            return null;
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
            } catch (SQLException e) {
                // Oh no, anyway...
                System.out.println("Errors closing results: " + e.getMessage());
            }
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                // This was not supposed to happen
                System.out.println("Errors closing statement: " + e.getMessage());
            }
        }
    }

    public void deleteTask(int id) {
        try {
            conn.setAutoCommit(false);
            deleteTask.setInt(1, id);
            int affectedRows = deleteTask.executeUpdate();
            if (affectedRows == 1) {
                conn.commit();
            } else {
                throw new SQLException("Task delete failed");
            }

        } catch (Exception e) {
            System.out.println("Delete Task failed : " + e.getMessage());
            try {
                System.out.println("Performing rollback");
                conn.rollback();
            } catch (SQLException e2) {
                System.out.println("Rollback failed, that's really bad : " + e2.getMessage());
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Could not reset autocommit : " + e.getMessage());
            }
        }

    }

    public boolean updateTask(String shortDescription, String longDescription, LocalDate date, int id) {
        try {
            conn.setAutoCommit(false);
            updateTask.setString(1, shortDescription);
            updateTask.setString(2, longDescription);
            String newDate = formatter.format(date);
            updateTask.setString(3, newDate);
            updateTask.setInt(4, id);
            int affectedRows = updateTask.executeUpdate();
            if (affectedRows == 1) {
                conn.commit();
                return true;
            } else {
                throw new SQLException("Task update failed");
            }

        } catch (Exception e) {
            System.out.println("Update Task failed : " + e.getMessage());
            e.printStackTrace();
            try {
                System.out.println("Performing rollback");
                conn.rollback();
            } catch (SQLException e2) {
                System.out.println("Rollback failed, that's really bad : " + e2.getMessage());
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Could not reset autocommit : " + e.getMessage());
            }
        }
        return false;
    }

    public boolean updateComplete(int id, int complete) {
        try {
            conn.setAutoCommit(false);
            updateComplete.setInt(1, complete);
            updateComplete.setInt(2, id);
            int affectedRows = updateComplete.executeUpdate();
            if (affectedRows == 1) {
                conn.commit();
                return true;
            } else {
                throw new SQLException("Task update failed");
            }

        } catch (Exception e) {
            System.out.println("Update Task failed : " + e.getMessage());
            e.printStackTrace();
            try {
                System.out.println("Performing rollback");
                conn.rollback();
            } catch (SQLException e2) {
                System.out.println("Rollback failed, that's really bad : " + e2.getMessage());
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Could not reset autocommit : " + e.getMessage());
            }
        }
        return false;

    }

    public String getCompleted() {
        Statement statement = null;
        int total;
        int completed;
        String result;
        try {
            statement = conn.createStatement();
            total = statement.executeQuery(COUNT_TASKS).getInt(1);
            completed = statement.executeQuery(COUNT_COMPLETED_TASKS).getInt(1);
            result = completed + "/" + total;
            return result;
        } catch (SQLException e) {
            System.out.println("Count failed " + e.getMessage());
            return null;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                // This was not supposed to happen
                System.out.println("Errors closing statement: " + e.getMessage());
            }
        }
    }
}

