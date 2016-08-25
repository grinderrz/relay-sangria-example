import Relay from 'react-relay';

export default class extends Relay.Route {
  static queries = {
    chat: () => Relay.QL`
      query {
        chat
      }
    `,
  };
  static routeName = 'AppHomeRoute';
}
