import streamlit as st
import requests

BASE_URL = "http://localhost:8082/api"

st.set_page_config(page_title="Register", page_icon="👤")
st.title("👤 Register")

with st.form("register_form"):
    email = st.text_input("Email")
    phone = st.text_input("Phone Number (with country code e.g. +91...)")
    channel = st.selectbox("OTP Channel", ["EMAIL", "SMS", "WHATSAPP"])
    submit = st.form_submit_button("Register")

    if submit:
        if not email and not phone:
            st.error("Please enter either email or phone number")
        else:
            payload = {
                "email": email if email else None,
                "phoneNumber": phone if phone else None,
                "channel": channel
            }

            try:
                response = requests.post(f"{BASE_URL}/auth/register", json=payload)
                data = response.json()

                if data["success"]:
                    st.success(data["message"])
                    # Store identifier and channel in session for OTP page
                    st.session_state["identifier"] = email if email else phone
                    st.session_state["channel"] = channel
                    st.session_state["purpose"] = "REGISTER"
                    st.info("Please go to OTP Verify page to complete registration")
                else:
                    st.error(f"{data['message']} - {data['errorCode']}")

            except Exception as e:
                st.error(f"Connection error: {str(e)}")