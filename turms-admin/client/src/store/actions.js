export default {
    setAdmin({commit}, admin) {
        commit('setAdmin', admin);
    },
    setUrl({commit}, url) {
        commit('setUrl', url);
    },
    clearAdmin({commit}) {
        commit('clearAdmin');
    }
};