export default {
    setAdmin(state, admin) {
        state.admin = admin;
    },
    setUrl(state, url) {
        state.url = url;
    },
    clearAdmin(state) {
        state.admin = null;
    }
};