import streamlit as st
import requests

BASE_URL = "http://localhost:8082/api"

st.set_page_config(page_title="Manage Users", page_icon="🚫")
st.title("🚫 Manage Users")

# Check if admin is logged in
if not st.session_state.get("admin_logged_in"):
    st.warning("⚠️ Please login as Admin first!")
    st.stop()

headers = {
    "Authorization": f"Bearer {st.session_state.get('admin_access_token')}"
}

# ================================================================
# SECTION 1 — Block User
# ================================================================
st.subheader("🔒 Block User")

with st.form("block_form"):
    user_id = st.number_input("User ID", min_value=1, step=1)
    reason = st.text_area("Reason for blocking")
    block_submit = st.form_submit_button("Block User")

    if block_submit:
        if not reason:
            st.error("Please provide a reason for blocking")
        else:
            payload = {
                "userId": user_id,
                "reason": reason
            }
            try:
                response = requests.post(
                    f"{BASE_URL}/admin/users/block",
                    json=payload,
                    headers=headers
                )
                data = response.json()

                if data["success"]:
                    st.success(f"✅ User {user_id} blocked successfully!")
                else:
                    st.error(f"{data['message']} - {data['errorCode']}")

            except Exception as e:
                st.error(f"Connection error: {str(e)}")

st.divider()

# ================================================================
# SECTION 2 — Unblock User
# ================================================================
st.subheader("🔓 Unblock User")

with st.form("unblock_form"):
    unblock_user_id = st.number_input("User ID to Unblock", min_value=1, step=1)
    unblock_submit = st.form_submit_button("Unblock User")

    if unblock_submit:
        try:
            response = requests.post(
                f"{BASE_URL}/admin/users/unblock",
                params={"userId": unblock_user_id},
                headers=headers
            )
            data = response.json()

            if data["success"]:
                st.success(f"✅ User {unblock_user_id} unblocked successfully!")
            else:
                st.error(f"{data['message']} - {data['errorCode']}")

        except Exception as e:
            st.error(f"Connection error: {str(e)}")

st.divider()

# ================================================================
# SECTION 3 — View All Blocked Users
# ================================================================
st.subheader("📋 Currently Blocked Users")

if st.button("🔄 Refresh Blocked Users"):
    st.rerun()

try:
    response = requests.get(
        f"{BASE_URL}/admin/users/blocked",
        headers=headers
    )
    data = response.json()

    if data["success"]:
        blocked_users = data["data"]

        if not blocked_users:
            st.info("No blocked users found")
        else:
            st.write(f"**Total Blocked: {len(blocked_users)}**")

            table_data = []
            for index, user in enumerate(blocked_users, start=1):
                table_data.append({
                    "S.No": index,
                    "User ID": user.get("user", {}).get("id"),
                    "Email": user.get("user", {}).get("email", "-"),
                    "Phone": user.get("user", {}).get("phoneNumber", "-"),
                    "Reason": user.get("reason"),
                    "Blocked At": user.get("blockedAt"),
                    "Unblocked At": user.get("unblockedAt", "-")
                })

            st.dataframe(
                table_data,
                use_container_width=True,
                hide_index=True
            )
    else:
        st.error(f"{data['message']} - {data['errorCode']}")

except Exception as e:
    st.error(f"Connection error: {str(e)}")