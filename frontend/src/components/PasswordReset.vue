<template>
    <div class="password-reset-enter-new" v-if="!isPasswordSuccessfullyReset">
        Please enter new password for {{login}}
        <input id="new-password" v-model="newPassword"/>
        <button id="set-password" @click="resetPassword()" class="blog-btn ok-btn">Set new password</button>
        <error v-show="errors.server" :message="errors.server"></error>
    </div>
    <div v-else>
        Now you can login with new password
    </div>
</template>

<script>
    import bus, {LOGIN} from '../bus'
    import {root_name} from '../routes'
    import Error from './Error.vue'

    const setNewPassword = '/api/password-reset-set-new';

    export default {
        data() {
            return {
                newPassword: null,
                isPasswordSuccessfullyReset: false,
                errors: {
                    server: null
                }
            }
        },
        components: {Error},
        methods: {
            resetPassword() {
                this.$http.post(setNewPassword, {passwordResetToken: this.$route.query.uuid, newPassword: this.$data.newPassword}).then(
                    goodResponse => {
                        this.$data.isPasswordSuccessfullyReset = true;
                    },
                    badResponse => {
                        console.log(badResponse);
                        this.$data.errors.server = badResponse.body.message;
                    }
                )
            },
            onSuccessLogin() {
                this.$router.push({ name: root_name });
            }
        },
        computed: {
            login(){
                return this.$route.query.login;
            }
        },
        created() {
            bus.$on(LOGIN, this.onSuccessLogin);
        },
        destroyed() {
            bus.$off(LOGIN, this.onSuccessLogin);
        }
    }
</script>