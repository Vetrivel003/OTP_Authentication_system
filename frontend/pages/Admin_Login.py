import streamlit as st
import requests

BASE_URL = "http://localhost:8082/api"

st.set_page_config(page_title="Admin Login", page_icon="🛡️")
st.title("🛡️ Admin Login")

# If already logged in as admin
if st.session_state.get("admin_logged_in"):
    st.success("✅ Already logged in as Admin!")
    st.info("Use the sidebar to navigate to Dashboard, Audit Logs or Manage Users")
    if st.button("Logout"):
        st.session_state.clear()
        st.rerun()
    st.stop()

with st.form("admin_login_form"):
    email = st.text_input("Admin Email")
    password = st.text_input("Password", type="password")
    submit = st.form_submit_button("Login")

    if submit:
        if not email or not password:
            st.error("Please enter email and password")
        else:
            payload = {
                "email": email,
                "password": password
            }

            try:
                response = requests.post(f"{BASE_URL}/admin/login", json=payload)
                data = response.json()

                if data["success"]:
                    # Store admin tokens in session
                    st.session_state["admin_access_token"] = data["data"]["accessToken"]
                    st.session_state["admin_refresh_token"] = data["data"]["refreshToken"]
                    st.session_state["admin_logged_in"] = True
                    st.session_state["admin_email"] = email
                    st.success("✅ Admin login successful!")
                    st.info("Use the sidebar to navigate to Dashboard")
                    st.rerun()
                else:
                    st.error(f"{data['message']} - {data['errorCode']}")

            except Exception as e:
                st.error(f"Connection error: {str(e)}")