def call(url) {
    call(url, 60, 3)
}

def call(url, wait, tries) {
    node() {
        try {
            echo "ping test ${url}"
            sh "wget --quiet --wait=$wait --tries=$tries --spider ${url} && echo 'Success' || echo 'Failure';"
        } catch (error) {
            throw error
        }
    }
}

return this;