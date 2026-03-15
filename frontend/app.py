import streamlit as st

st.set_page_config(
    page_title="OTP Authentication System",
    page_icon="🔐",
    layout="centered"
)

st.title("🔐 OTP Authentication System")
st.markdown("""
### Welcome!
Use the sidebar to navigate:

**User:**
- 👤 Register
- 🔑 Login
- ✅ Verify OTP

**Admin:**
- 🛡️ Admin Login
- 📊 Dashboard
- 📋 Audit Logs
- 🚫 Manage Users
""")