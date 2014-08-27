requirejs.config({
    paths: {
        'myunderscore': '../lib/underscorejs/underscore',
        myknockout: '../lib/knockout/knockout'
    },
    shim: {
        'underscore': {
            exports: '_'
        }
    }
});

require(["./a"], function() {

});