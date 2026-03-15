import streamlit as st
import requests

BASE_URL = "http://localhost:8082/api"

st.set_page_config(page_title="Admin Dashboard", page_icon="📊")
st.title("📊 Admin Dashboard")

# Check if admin is logged in
if not st.session_state.get("admin_logged_in"):
    st.warning("⚠️ Please login as Admin first!")
    st.stop()

st.success(f"Welcome, **{st.session_state.get('admin_email')}**!")

headers = {
    "Authorization": f"Bearer {st.session_state.get('admin_access_token')}"
}

# Auto refresh button
if st.button("🔄 Refresh Stats"):
    st.rerun()

try:
    response = requests.get(f"{BASE_URL}/admin/logs/stats", headers=headers)
    data = response.json()

    if data["success"]:
        stats = data["data"]

        # Display stats in metric cards
        col1, col2, col3, col4 = st.columns(4)

        with col1:
            st.metric(
                label="📨 Total Requests",
                value=stats["totalRequests"]
            )

        with col2:
            st.metric(
                label="✅ Success Count",
                value=stats["successCount"]
            )

        with col3:
            st.metric(
                label="❌ Failed Deliveries",
                value=stats["failedDeliveries"]
            )

        with col4:
            st.metric(
                label="🔄 Active Sessions",
                value=stats["activeSessions"]
            )

        st.divider()

        # Success rate progress bar
        st.subheader("📈 Success Rate")
        success_rate = stats["successRate"]
        st.progress(success_rate / 100)
        st.write(f"**{success_rate}%** of all OTP requests were successful")

    else:
        st.error(f"{data['message']} - {data['errorCode']}")

except Exception as e:
    st.error(f"Connection error: {str(e)}")