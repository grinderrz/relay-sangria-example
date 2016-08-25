import React from 'react';
import Relay from 'react-relay';
import * as RelaySubscriptions from 'relay-subscriptions';
import MessageAddedSubscription from '../subscriptions/MessageAddedSubscription';
import AddMessageMutation from '../mutations/AddMessageMutation';
import MessageTextInput from './MessageTextInput';

class App extends React.Component {
  _handleTextInputSave = (text) => {
    /*
    this.props.relay.setVariables({
      last: this.props.relay.variables.last + 1,
    }, (a) => { console.log("orsc", a); });
    */
    this.props.relay.commitUpdate(
      new AddMessageMutation({message: text, chat: this.props.chat, chatId: "1"}),
      {
        onFailure: () => {
          /*
          this.props.relay.setVariables({
            last: this.props.relay.variables.last - 1
          });
          */
        }
      })
  }
  componentDidMount() {
    const subscribe = this.props.subscriptions.subscribe;
    this._addSubscription = subscribe(
      new MessageAddedSubscription({ chat: this.props.chat }),
      (data) => {
        this.props.relay.setVariables({
          last: this.props.relay.variables.last + 1
        });
        //console.log("onsub", data);
      }
    );
  }
  componentWillUnmount() {
    if (this._addSubscription) this._addSubscription.dispose();
  }
  /*
  componentWillReceiveProps(np) {
    console.log("cwrp", np, this.props);
  }
  */
  _onclickHistory = () => {
    this.props.relay.setVariables({
      last: this.props.relay.variables.last + 5
    });
  }
  renderMessages() {
    return this.props.chat.messages.edges.map(edge =>
      <div key={edge.node.id}>{edge.node.content} (ID: {edge.node.rawId})</div>
    );
  }
  render() {
    return (
      <div>
        <h1>Messages</h1>
        { this.props.chat.messages.pageInfo.hasPreviousPage ? 
          <input type="button" name="history" onClick={this._onclickHistory} value="earlier messages"></input> : null
        }
        <div>
          {this.renderMessages()}
        </div>
        <MessageTextInput
          autoFocus={true}
          className="new-message"
          onSave={this._handleTextInputSave}
          placeholder="new message"
        />
      </div>
    );
  }
}

export default Relay.createContainer(RelaySubscriptions.createSubscriptionContainer(App), {
  initialVariables: {
    last: 5,
  },
  fragments: {
    chat: () => Relay.QL`
      fragment on Chat {
        messages(last: $last) {
          pageInfo {
            hasPreviousPage
            endCursor
          }
          edges {
            node {
              id,
              rawId,
              content,
            },
          },
        },
      }
    `,
  },
});
