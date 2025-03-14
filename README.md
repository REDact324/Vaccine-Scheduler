# Vaccine-Scheduler
An vaccine scheduler to manage reservation for caregiver and patients.

## Guide
Once the user runs the program, these are all the possible options that can be done (assuming they are allowed to). I will run through what each option does.
```
Welcome to the COVID-19 Vaccine Reservation Scheduling Application!
*** Please enter one of the following commands ***
> create_patient <username> <password>
> create_caregiver <username> <password>
> login_patient <username> <password>
> login_caregiver <username> <password>
> search_caregiver_schedule <date>
> reserve <date> <vaccine>
> upload_availability <date>
> cancel <appointment_id>
> add_doses <vaccine> <number>
> show_appointments
> logout
> quit
```
- create_patient and create_caregiver allows the user to create a patient to receive the vaccine or a caregiver to adminster it. Note that a caregiver does not require any medical license or degree as this is purely a simulation. Passwords' strength are checked using Regex and then hashed using the SHA-256 algorithm if valid.
- login_patient and login_caregiver allows the user to login as an existing patient and caregiver.
- search_caregiver_schedule allows a caregiver or patient to search for caregivers available on the given date as well as the number of doses of each vaccine left.
- reserve allows a patient to reserve a valid date and vaccine (assuming there are doses left) for an appointment with a caregiver that day. The caregiver is chosen in ascending alphabetical order.
- upload_availability allows caregivers to upload a date when they are available for patients to make an appointment with them.
- cancel allows both patients and caregivers to cancel a valid date they have an appointment on.
- show_all_available_dates shows all available dates for every caregiver.
- add_doses allows caregivers to add doses to existing vaccines or to create a new vaccine (real or fiction).
- show_appointments shows appointments for the logged in patient or caregiver
- logout is self-explanatory
- quit terminates the program.

## Requirements / Installation
1. Clone the repo into an IDE (IntelliJ IDEA with Java 11 is the developmenting environment and recommanded)
2. Create a database server (Microsoft Azure heavily recommended as it works well with pymssql).
3. Run Scheduler.java
