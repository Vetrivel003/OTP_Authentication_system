# pages/6_Audit_Logs.py
import streamlit as st
import requests

BASE_URL = "http://localhost:8082/api"

st.set_page_config(page_title="Audit Logs", page_icon="📋")
st.title("📋 Audit Logs")

# Check if admin is logged in
if not st.session_state.get("admin_logged_in"):
    st.warning("⚠️ Please login as Admin first!")
    st.stop()

headers = {
    "Authorization": f"Bearer {st.session_state.get('admin_access_token')}"
}

# Filters
st.subheader("🔍 Filters")
col1, col2, col3 = st.columns(3)

with col1:
    page_number = st.number_input("Page", min_value=0, value=0, step=1)

with col2:
    page_size = st.selectbox("Page Size", [10, 25, 50, 100])

with col3:
    st.write("")
    st.write("")
    refresh = st.button("🔄 Fetch Logs")

st.divider()

# Fetch logs
try:
    response = requests.get(
        f"{BASE_URL}/admin/logs",
        headers=headers,
        params={
            "page": page_number,
            "size": page_size,
            "sort": "id,asc"
        }
    )
    data = response.json()

    if data["success"]:
        logs = data["data"]
        total = logs["totalElements"]
        content = logs["content"]

        st.write(f"**Total Logs: {total}**")

        if not content:
            st.info("No logs found")
        else:
            # Build table data
            table_data = []
            for log in content:
                table_data.append({
                    "ID": log.get("id"),
                    "Event": log.get("eventType"),
                    "Status": log.get("status"),
                    "Channel": log.get("channel", "-"),
                    "Identifier": log.get("identifier", "-"),
                    "IP Address": log.get("ipAddress", "-"),
                    "Error": log.get("errorMessage", "-"),
                    "Timestamp": log.get("createdAt")
                })

            st.dataframe(
                table_data,
                use_container_width=True,
                hide_index=True
            )

            # Pagination info
            st.write(
                f"Page **{logs['number'] + 1}** of **{logs['totalPages']}**"
            )

    else:
        st.error(f"{data['message']} - {data['errorCode']}")

except Exception as e:
    st.error(f"Connection error: {str(e)}")