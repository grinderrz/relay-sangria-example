import 'babel-polyfill';

import App from './components/App';
import AppHomeRoute from './routes/AppHomeRoute';
import React from 'react';
import ReactDOM from 'react-dom';
import Relay from 'react-relay';
import RelayNetworkDebug from 'react-relay/lib/RelayNetworkDebug';

import Root from './components/Root';

RelayNetworkDebug.init();

ReactDOM.render(
  <Root environment={Relay.Store}>
    <Relay.RootContainer
      Component={App}
      route={new AppHomeRoute()}
      />
  </Root>,
  document.getElementById('root')
);
