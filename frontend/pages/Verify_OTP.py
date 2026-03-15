import streamlit as st
import requests

BASE_URL = "http://localhost:8082/api"

st.set_page_config(page_title="Verify OTP", page_icon="✅")
st.title("✅ Verify OTP")

# Check if session has identifier and channel
identifier = st.session_state.get("identifier", "")
channel = st.session_state.get("channel", "")
purpose = st.session_state.get("purpose", "")

if not identifier or not channel:
    st.warning("Please Register or Login first!")
    st.stop()

st.info(f"OTP sent to: **{identifier}** via **{channel}**")

with st.form("verify_form"):
    otp = st.text_input("Enter OTP", max_chars=6, placeholder="Enter 6-digit OTP")
    submit = st.form_submit_button("Verify OTP")

    if submit:
        if not otp:
            st.error("Please enter OTP")
        elif len(otp) != 6:
            st.error("OTP must be 6 digits")
        else:
            payload = {
                "identifier": identifier,
                "channel": channel,
                "otp": otp,
                "purpose": purpose  # REGISTER or LOGIN
            }

            try:
                response = requests.post(f"{BASE_URL}/otp/verify", json=payload)
                data = response.json()

                if data["success"]:
                    if purpose == "REGISTER":
                        st.success("✅ Registration verified successfully!")
                        st.info("Please go to Login page to continue")
                        # Clear session
                        st.session_state.clear()

                    elif purpose == "LOGIN":
                        st.success("✅ Login successful!")
                        # Store JWT tokens in session
                        st.session_state["access_token"] = data["data"]["accessToken"]
                        st.session_state["refresh_token"] = data["data"]["refreshToken"]
                        st.session_state["is_logged_in"] = True
                        st.info("You are now logged in!")
                        # Clear OTP session data
                        st.session_state.pop("identifier", None)
                        st.session_state.pop("channel", None)
                        st.session_state.pop("purpose", None)
                else:
                    st.error(f"{data['message']} - {data['errorCode']}")

            except Exception as e:
                st.error(f"Connection error: {str(e)}")

# Resend OTP button
st.divider()
st.subheader("Didn't receive OTP?")
if st.button("Resend OTP"):
    payload = {
        "identifier": identifier,
        "channel": channel
    }
    try:
        response = requests.post(f"{BASE_URL}/otp/resend", json=payload)
        data = response.json()
        if data["success"]:
            st.success("OTP resent successfully!")
        else:
            st.error(f"{data['message']} - {data['errorCode']}")
    except Exception as e:
        st.error(f"Connection error: {str(e)}")
