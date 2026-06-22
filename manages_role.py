import firebase_admin
from firebase_admin import credentials, auth
import os

# Replace 'path/to/your-service-account-key.json' with the actual path
# Or set GOOGLE_APPLICATION_CREDENTIALS environment variable
# For simplicity, let's assume the key is in the same directory for this script example
SERVICE_ACCOUNT_KEY_PATH = './myschedule-c917e-firebase-adminsdk-fbsvc-6ed7e5f9a0.json'
# Example: If the key is 'myschedule-c917e-firebase-adminsdk-xxxxx-xxxxxx.json'
# SERVICE_ACCOUNT_KEY_PATH = 'myschedule-c917e-firebase-adminsdk-xxxxx-xxxxxx.json'

# --- Initialize Firebase Admin SDK ---
if not firebase_admin._apps: # Check if app is already initialized
    try:
        cred = credentials.Certificate(SERVICE_ACCOUNT_KEY_PATH)
        firebase_admin.initialize_app(cred)
        print("Firebase Admin SDK initialized successfully.")
    except Exception as e:
        print(f"Error initializing Firebase Admin SDK: {e}")
        print("Please ensure the service account key path is correct and the file exists.")
        exit()

def set_teacher_role(email_or_uid):
    """Sets the 'teacher' custom claim for a user."""
    try:
        user = auth.get_user_by_email(email_or_uid)
        # If email lookup fails, try UID
    except auth.UserNotFoundError:
        try:
            user = auth.get_user(email_or_uid)
        except auth.UserNotFoundError:
            print(f"User with email or UID '{email_or_uid}' not found.")
            return

    # Set custom claims
    auth.set_custom_user_claims(user.uid, {'role': 'teacher'})
    print(f"Successfully set 'teacher' role for user: {user.email or user.uid}")
    print("User's ID token will reflect new claims after a short delay or next sign-in.")

def get_user_claims(email_or_uid):
    """Retrieves custom claims for a user."""
    try:
        user = auth.get_user_by_email(email_or_uid)
    except auth.UserNotFoundError:
        try:
            user = auth.get_user(email_or_uid)
        except auth.UserNotFoundError:
            print(f"User with email or UID '{email_or_uid}' not found.")
            return

    print(f"Claims for user {user.email or user.uid}: {user.custom_claims}")

if __name__ == "__main__":
    while True:
        action = input("\nEnter 'set' to set teacher role, 'get' to view claims, or 'quit': ").lower()
        if action == 'quit':
            break
        elif action == 'set':
            user_input = input("Enter user's email or UID to set as teacher: ")
            set_teacher_role(user_input)
        elif action == 'get':
            user_input = input("Enter user's email or UID to get claims: ")
            get_user_claims(user_input)
        else:
            print("Invalid action.")

