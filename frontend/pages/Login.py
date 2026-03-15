import streamlit as st
import requests

BASE_URL = "http://localhost:8082/api"

st.set_page_config(page_title="Login", page_icon="🔑")
st.title("🔑 Login")

with st.form("login_form"):
    identifier = st.text_input("Email or Phone Number")
    channel = st.selectbox("OTP Channel", ["EMAIL", "SMS", "WHATSAPP"])
    submit = st.form_submit_button("Send OTP")

    if submit:
        if not identifier:
            st.error("Please enter email or phone number")
        else:
            payload = {
                "identifier": identifier,
                "channel": channel
            }

            try:
                response = requests.post(f"{BASE_URL}/auth/login", json=payload)
                data = response.json()

                if data["success"]:
                    st.success(data["message"])
                    # Store in session for OTP verify page
                    st.session_state["identifier"] = identifier
                    st.session_state["channel"] = channel
                    st.session_state["purpose"] = "LOGIN"
                    st.info("Please go to OTP Verify page to complete login")
                else:
                    st.error(f"{data['message']} - {data['errorCode']}")

            except Exception as e:
                st.error(f"Connection error: {str(e)}")